<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://rhn.redhat.com/rhn" prefix="rhn"%>
<%@ taglib uri="http://rhn.redhat.com/tags/list" prefix="rl"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<html:html>
    <body>
        <rhn:toolbar base="h1" icon="header-info">
            <bean:message key="iss.confirmmasterremove.jsp.toolbar" />
        </rhn:toolbar>
        <p><bean:message key="iss.confirmmasterremove.jsp.areyousure" /></p>
        <rhn:dialogmenu mindepth="0" maxdepth="1"
                        definition="/WEB-INF/nav/iss_config.xml"
                        renderer="com.redhat.rhn.frontend.nav.DialognavRenderer" />
        <c:set var="pageList" value="${requestScope.all}" />
        <h2><bean:message key="iss.confirmmasterremove.jsp.header2" /></h2>
        <rl:listset name="issMasterListSet">
            <rhn:csrf />
            <rhn:submitted />
            <c:if test="${not empty requestScope.all}">
                <div class="spacewalk-section-toolbar">
                    <div class="action-button-wrapper">
                        <input type="submit" name="dispatch" class="btn btn-danger"
                            value='<bean:message key="iss.confirm.remove.masters"/>' />
                    </div>
                </div>
            </c:if>
            <rl:list dataset="pageList" name="issMasterList"
                     emptykey="iss.slave.jsp.nomasters">
                <rl:column sortable="true" headerkey="iss.master.label" sortattr="label">
                    <html:link href="/rhn/admin/iss/EditMaster.do?mid=${current.id}">
                        <c:out value="${current.label}" />
                    </html:link>
                </rl:column>
            </rl:list>
        </rl:listset>
    </body>
</html:html>
