package com.example.data.extension.mangayomi.bridge

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * JS-accessible wrapper around Jsoup Element.
 */
class DomElementWrapper(val element: Element) {

    val text: String
        get() = element.text()

    val textContent: String
        get() = element.text()

    val outerHtml: String
        get() = element.outerHtml()

    val innerHtml: String
        get() = element.html()

    val tagName: String
        get() = element.tagName()

    val className: String
        get() = element.className()

    val id: String
        get() = element.id()

    val href: String
        get() = element.attr("href")

    val src: String
        get() = element.attr("src")

    fun text(): String = element.text()

    fun html(): String = element.html()

    fun attr(attributeKey: String): String = element.attr(attributeKey)

    fun getAttribute(attributeKey: String): String = element.attr(attributeKey)

    fun hasAttr(attributeKey: String): Boolean = element.hasAttr(attributeKey)

    fun querySelector(cssQuery: String): DomElementWrapper? {
        val found = element.selectFirst(cssQuery) ?: return null
        return DomElementWrapper(found)
    }

    fun selectFirst(cssQuery: String): DomElementWrapper? = querySelector(cssQuery)

    fun querySelectorAll(cssQuery: String): Array<DomElementWrapper> {
        val list = element.select(cssQuery)
        return list.map { DomElementWrapper(it) }.toTypedArray()
    }

    fun select(cssQuery: String): Array<DomElementWrapper> = querySelectorAll(cssQuery)

    fun getElementsByTagName(tagName: String): Array<DomElementWrapper> {
        val list = element.getElementsByTag(tagName)
        return list.map { DomElementWrapper(it) }.toTypedArray()
    }

    fun getElementsByClassName(className: String): Array<DomElementWrapper> {
        val list = element.getElementsByClass(className)
        return list.map { DomElementWrapper(it) }.toTypedArray()
    }

    fun getElementById(id: String): DomElementWrapper? {
        val el = element.getElementById(id) ?: return null
        return DomElementWrapper(el)
    }

    fun children(): Array<DomElementWrapper> {
        return element.children().map { DomElementWrapper(it) }.toTypedArray()
    }

    fun parent(): DomElementWrapper? {
        val p = element.parent() ?: return null
        return DomElementWrapper(p)
    }

    val parentElement: DomElementWrapper?
        get() = parent()

    val parentNode: DomElementWrapper?
        get() = parent()

    val firstElementChild: DomElementWrapper?
        get() = element.firstElementChild()?.let { DomElementWrapper(it) }

    val lastElementChild: DomElementWrapper?
        get() = element.lastElementChild()?.let { DomElementWrapper(it) }

    fun nextElementSibling(): DomElementWrapper? {
        val next = element.nextElementSibling() ?: return null
        return DomElementWrapper(next)
    }

    fun previousElementSibling(): DomElementWrapper? {
        val prev = element.previousElementSibling() ?: return null
        return DomElementWrapper(prev)
    }

    /**
     * Basic XPath evaluation converted to CSS/DOM traversal.
     */
    fun xpath(xpathExpr: String): Array<DomElementWrapper> {
        val css = xpathToCss(xpathExpr)
        return if (css.isNotBlank()) {
            querySelectorAll(css)
        } else {
            emptyArray()
        }
    }
}

/**
 * JS-accessible wrapper around Jsoup Document.
 */
class DomDocumentWrapper(val document: Document) {

    constructor(html: String) : this(Jsoup.parse(html))

    val body: DomElementWrapper
        get() = DomElementWrapper(document.body())

    val head: DomElementWrapper
        get() = DomElementWrapper(document.head())

    val title: String
        get() = document.title()

    fun querySelector(cssQuery: String): DomElementWrapper? {
        val found = document.selectFirst(cssQuery) ?: return null
        return DomElementWrapper(found)
    }

    fun selectFirst(cssQuery: String): DomElementWrapper? = querySelector(cssQuery)

    fun querySelectorAll(cssQuery: String): Array<DomElementWrapper> {
        val list = document.select(cssQuery)
        return list.map { DomElementWrapper(it) }.toTypedArray()
    }

    fun select(cssQuery: String): Array<DomElementWrapper> = querySelectorAll(cssQuery)

    fun getElementsByTagName(tagName: String): Array<DomElementWrapper> {
        val list = document.getElementsByTag(tagName)
        return list.map { DomElementWrapper(it) }.toTypedArray()
    }

    fun getElementsByClassName(className: String): Array<DomElementWrapper> {
        val list = document.getElementsByClass(className)
        return list.map { DomElementWrapper(it) }.toTypedArray()
    }

    fun getElementById(id: String): DomElementWrapper? {
        val el = document.getElementById(id) ?: return null
        return DomElementWrapper(el)
    }

    /**
     * Basic XPath evaluation for Mangayomi Document.
     */
    fun xpath(xpathExpr: String): Array<DomElementWrapper> {
        val css = xpathToCss(xpathExpr)
        return if (css.isNotBlank()) {
            querySelectorAll(css)
        } else {
            emptyArray()
        }
    }
}

/**
 * Basic converter from common XPath expressions to CSS selectors.
 */
internal fun xpathToCss(xpath: String): String {
    var css = xpath.trim()
    if (css.startsWith("//")) {
        css = css.substring(2)
    } else if (css.startsWith("/")) {
        css = css.substring(1)
    }
    // Replace [@attr='val'] with [attr='val']
    css = css.replace(Regex("@([a-zA-Z0-9_-]+)"), "$1")
    // Replace contains(@class, 'xyz') with .xyz or [class*='xyz']
    css = css.replace(Regex("contains\\(class,\\s*['\"]([^'\"]+)['\"]\\)"), "class*='$1'")
    // Replace contains(@attr, 'xyz') with [attr*='xyz']
    css = css.replace(Regex("contains\\(([a-zA-Z0-9_-]+),\\s*['\"]([^'\"]+)['\"]\\)"), "$1*='$2'")
    // Replace slashes with spaces for descendants
    css = css.replace("/", " ")
    return css.trim()
}

/**
 * Factory providing DOM parsing utilities to the JS environment.
 */
class MangayomiDomBridge {
    fun parseHtml(html: String): DomDocumentWrapper = DomDocumentWrapper(html)
    fun createDocument(html: String): DomDocumentWrapper = DomDocumentWrapper(html)
    fun Document(html: String): DomDocumentWrapper = DomDocumentWrapper(html)
}

