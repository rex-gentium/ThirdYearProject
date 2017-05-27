form=document.getElementById("loginForm");

function submitWithAction(action) {
        form.action=action;
        form.submit();
}