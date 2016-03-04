'use strict';

const React = require("react");
const CustomStatesModule = require("../components/custom-states")
const CustomStates = CustomStatesModule.CustomStates;
const msg = CustomStatesModule.msg;


function matchUrl(target) {
    return "/rhn/manager/api/states/match?id=" + groupId + "&type=group"
             + (target ? "&target=" + target : "");
}

function applyRequest(component) {
    return $.ajax({
        type: "POST",
        url: "/rhn/manager/api/states/apply",
        data: JSON.stringify({
            id: groupId,
            type: "group",
            states: ["custom"]
        }),
        contentType: "application/json",
        dataType: "json"
    })
    .done( data => {
          console.log("apply action queued:" + data)
          component.setState({
              messages: msg('info', <span>{t("Applying the custom states has been scheduled for each minion server in this group")}</span>)
          });
      });
}

function saveRequest(states) {
    return $.ajax({
        type: "POST",
        url: "/rhn/manager/api/states/save",
        data: JSON.stringify({
            id: groupId,
            type: "group",
            saltStates: states
        }),
        contentType: "application/json"
    })
}

React.render(
  <CustomStates matchUrl={matchUrl} saveRequest={saveRequest} applyRequest={applyRequest}/>,
  document.getElementById('custom-states')
);
