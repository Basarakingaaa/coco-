$(function () {
    $("#loginForm").on("submit", function (event) {
        event.preventDefault();

        const $button = $("#loginButton");
        const $message = $("#message");

        $button.prop("disabled", true).text("登录中...");
        $message.removeClass("error success").text("");

        $.ajax({
            url: "api/login",
            type: "POST",
            data: {
                username: $("#username").val(),
                password: $("#password").val()
            },
            success: function (result) {
                $message.addClass("success").text(result.message || "登录成功");
                window.location.href = result.redirect || "home.html";
            },
            error: function (xhr) {
                const result = xhr.responseJSON || {};
                $message.addClass("error").text(result.message || "登录失败");
            },
            complete: function () {
                $button.prop("disabled", false).text("登录");
            }
        });
    });
});
