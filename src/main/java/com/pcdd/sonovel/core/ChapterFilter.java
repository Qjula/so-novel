package com.pcdd.sonovel.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import com.pcdd.sonovel.model.AppConfig;
import com.pcdd.sonovel.model.Chapter;

/**
 * @author pcdd
 * Created at 2024/3/17
 */
public class ChapterFilter extends Source {

    public ChapterFilter(AppConfig config) {
        super(config);
    }

    /**
     * 过滤正文内容
     */
    public String filter(Chapter chapter) {
        return new FilterBuilder(chapter)
                .filterEscape(true)
                .filterAds(true)
                .filterDuplicateTitle(true)
                .build();
    }

    /**
     * 建造者类，用于动态组合过滤步骤
     */
    public class FilterBuilder {
        private final String title;
        private String content;
        private boolean applyEscapeFilter;
        private boolean applyAdsFilter;
        private boolean applyDuplicateTitleFilter;

        public FilterBuilder(Chapter chapter) {
            this.title = chapter.getTitle();
            this.content = chapter.getContent();
        }

        /**
         * 是否启用 HTML 实体字符过滤
         */
        public FilterBuilder filterEscape(boolean apply) {
            this.applyEscapeFilter = apply;
            return this;
        }

        /**
         * 是否启用广告过滤
         */
        public FilterBuilder filterAds(boolean apply) {
            this.applyAdsFilter = apply;
            return this;
        }

        /**
         * 是否启用重复标题过滤
         */
        public FilterBuilder filterDuplicateTitle(boolean apply) {
            this.applyDuplicateTitleFilter = apply;
            return this;
        }

        /**
         * 构建最终过滤内容
         */
        public String build() {
            if (applyEscapeFilter) {
                // 替换 &..; (HTML 字符实体引用)，主要是 &nbsp;，可能会导致 ibooks 章节报错
                this.content = this.content.replaceAll("&[^;]+;", "");
            }

            if (applyAdsFilter) {
                String filteredContent = this.content.replaceAll(rule.getChapter().getFilterTxt(), "");
                this.content = HtmlUtil.removeHtmlTag(filteredContent, StrUtil.splitToArray(rule.getChapter().getFilterTag(), " "));
            }

            // 确保在 EscapeFilter、AdsFilter 之后
            this.content = StrUtil.cleanBlank(this.content);

            if (applyDuplicateTitleFilter) {
                String noBlankTitle = StrUtil.cleanBlank(this.title);
                // 正文开头的重复标题
                String regexTemplate = "^(?:<.*?>%s</.*?>|%s)";
                this.content = content.replaceFirst(StrUtil.format("{}|{}",
                        regexTemplate.formatted(title, title),
                        regexTemplate.formatted(noBlankTitle, noBlankTitle)), "");
            }

            // 删除全部空标签，例如 <p></p>
            return this.content.replaceAll("<(\\w+)([^>]*)>\\s*</\\1>", "");
        }
    }

}