title: 在liftweb中扩展一个类似jQuery的方法
date: 2013-01-01 22:10:18
categories: scala
tags:
- scala
- liftweb
- jquery
---

***先上代码，文字稍后再补！***

``` scala
    import scala.xml.NodeSeq
     
    import net.liftweb.http.js._
    import net.liftweb.http.js.jquery._
     
    def $(exp: String): jQuery = $(JE.Str(exp))
     
    def $(exp: JsExp): jQuery = jQuery(exp)
     
    case class jQuery(exp: JsExp) {
      val jq = JqJE.Jq(exp)
     
      @inline
      def value(value: JsExp) = (jq ~> JqJE.JqAttr("value", value)).cmd
     
      @inline
      def value() = (jq ~> JqJE.JqGetAttr("value")).cmd
     
      @inline
      def html(content: NodeSeq) = (jq ~> JqJE.JqHtml(content)).cmd
     
      @inline
      def html() = (jq ~> JqJE.JqHtml()).cmd
     
      @inline
      def remove() = (jq ~> JqJE.JqRemove()).cmd
     
      @inline
      def attr(key: String, value: JsExp) = (jq ~> JqJE.JqAttr(key, value)).cmd
     
      @inline
      def attr(key: String) = (jq ~> JqJE.JqGetAttr(key)).cmd
     
      @inline
      def removeAttr(key: String): JsCmd =
        (jq ~> JqRemoveAttr(key)).cmd
     
      // 更多方法实现 ................................................................
     
      case class JqRemoveAttr(key: String) extends JsExp with JsMember {
        def toJsCmd = "removeAttr(" + JE.Str(key) + ")"
      }
     
    }
```

