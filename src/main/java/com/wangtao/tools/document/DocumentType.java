package com.wangtao.tools.document;

/**
 * @author wangtao
 * Created at 2026-11-21
 */
public enum DocumentType {

    HTML,

    WORD,

    FAKE_WORD,

    /**
     * 基于 flying-saucer 的 PDF
     */
    PDF,

    /**
     * 基于 openhtmltopdf 的 PDF
     */
    OPENHTMLTOPDF
}
