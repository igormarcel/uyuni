<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://rhn.redhat.com/rhn" prefix="rhn" %>
<%@ taglib uri="http://rhn.redhat.com/tags/list" prefix="rl" %>

<html>
    <head>
        <script type="text/javascript" src="/rhn/dwr/interface/MirrorCredentialsRenderer.js"></script>
        <script type="text/javascript" src="/rhn/dwr/engine.js"></script>
        <script type="text/javascript">
          function initEdit(id, email, user) {
            console.log("initEdit(): " + id);
            $('#modal-id').val(id);
            $('#modal-email').val(email);
            $('#modal-user').val(user);
            $('#modal-password').val("");
          }
          function initDelete(id, email, user) {
            console.log("initDelete(): " + id);
            $('#delete-id').val(id);
            $('#delete-email').text(email);
            $('#delete-user').text(user);
          }
          function hideModal() {
              console.log("hideModal()");
              $('#edit-credentials-modal').modal('hide');
              $('#delete-credentials-modal').modal('hide');
          }
          function saveCredentials() {
              // Read values
              var id = $('#modal-id').val();
              var email = $('#modal-email').val();
              var user = $('#modal-user').val();
              var password = $('#modal-password').val();
              initEdit("", "", "");
              console.log("Saving credentials: " + id);
              MirrorCredentialsRenderer.saveCredentials(id, email, user, password,
                  makeAjaxCallback("listset-container", false));
          }
          function deleteCredentials() {
            var id = $('#delete-id').val();
            $("#delete-" + id).html("<i class='fa fa-spinner fa-spin'></i>");
            MirrorCredentialsRenderer.deleteCredentials(id,
                makeAjaxCallback("listset-container", false));
          }
          function downloadSubscriptions(id) {
            $("#subscriptions-" + id).html("<i class='fa fa-spinner fa-spin'></i>");
            MirrorCredentialsRenderer.renderSubscriptions(id,
                makeAjaxCallback("subscriptions-" + id, false));
          }
        </script>
    </head>
    <body>
        <!-- BEGIN EDIT MODAL -->
        <div class="modal fade" id="edit-credentials-modal">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                        <h4 class="modal-title">Edit Credentials</h4>
                    </div>
                    <div class="modal-body">
                        <form class="form-horizontal" role="form">
                            <input type="hidden" id="modal-id">
                            <div class="form-group">
                                <label for="modal-email" class="col-sm-2 control-label">Email</label>
                                <div class="col-sm-10">
                                    <input type="email" class="form-control" id="modal-email" placeholder="Email">
                                </div>
                            </div>
                            <div class="form-group">
                                <label for="modal-user" class="col-sm-2 control-label">User</label>
                                <div class="col-sm-10">
                                    <input type="text" class="form-control" id="modal-user" placeholder="Username">
                                </div>
                            </div>
                            <div class="form-group">
                                <label for="modal-password" class="col-sm-2 control-label">Password</label>
                                <div class="col-sm-10">
                                    <input type="password" class="form-control" id="modal-password" placeholder="&bull;&bull;&bull;&bull;&bull;&bull;">
                                </div>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                        <button type="button" class="btn btn-success" onClick="saveCredentials();">Save credentials</button>
                    </div>
                </div>
            </div>
        </div>
        <!-- END EDIT MODAL -->

        <!-- BEGIN DELETE MODAL -->
        <div class="modal fade" id="delete-credentials-modal">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                        <h4 class="modal-title">Delete Credentials</h4>
                    </div>
                    <div class="modal-body">
                        <p>You are about to delete the credentials for:</p>
                        <form class="form-horizontal" role="form">
                            <input type="hidden" id="delete-id">
                            <div class="form-group">
                                <label class="col-sm-2 control-label">Email</label>
                                <div class="col-sm-10">
                                    <p class="form-control-static" id="delete-email"></p>
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="col-sm-2 control-label">User</label>
                                <div class="col-sm-10">
                                    <p class="form-control-static" id="delete-user"></p>
                                </div>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                        <button type="button" class="btn btn-success" onClick="deleteCredentials();">Delete credentials</button>
                    </div>
                </div>
            </div>
        </div>
        <!-- END DELETE MODAL -->

        <rhn:toolbar base="h1" icon="header-preferences">
            <bean:message key="Setup Wizard" />
        </rhn:toolbar>
        <rhn:dialogmenu mindepth="0" maxdepth="1"
                        definition="/WEB-INF/nav/setup_wizard.xml"
                        renderer="com.redhat.rhn.frontend.nav.DialognavRenderer" />
        <p>
            Test and edit your mirror credentials below or <a href="javascript:void(0);" data-toggle="modal" data-target="#edit-credentials-modal">add some</a>.
        </p>
        <div id="listset-container">
            <i class='fa fa-spinner fa-spin'></i><span>Loading ...</span>
            <script>
                MirrorCredentialsRenderer.renderCredentials(makeAjaxCallback("listset-container", false));
            </script>
        </div>
        <div class="pull-right">
            <hr />
            <a class="btn btn-success" href="/rhn/admin/setup/SUSEProducts.do">
                <bean:message key="mirror-credentials.jsp.dispatch" />
            </a>
        </div>
    </body>
</html>
