$(function () {
    $.ajax({
        url: "api/current-user",
        type: "GET",
        success: function (result) {
            const user = result.user;
            $("#username").text(user.username);
            $("#userId").text(user.id);
            $("#userNameText").text(user.username);
            $("#role").text(user.role);
        },
        error: function () {
            window.location.href = "login.html";
        }
    });

    $("#logoutButton").on("click", function () {
        $.post("api/logout").always(function () {
            window.location.href = "login.html";
        });
    });
});
