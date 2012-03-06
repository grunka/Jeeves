<!DOCTYPE html>
<html>
<head>
    <title>Hello</title>
    <script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js"></script>
    <script type="text/javascript">
        function jsonPost(url, data, success) {
            $.ajax({
                url:url,
                type:"POST",
                data:JSON.stringify(data),
                dataType:"json",
                contentType:"application/json; charset=utf-8",
                success:success
            });
        }
        $(function () {
            var d = {"value":{"b":true}};
            $.post("rpc/service/complex", d, function (data) {
                $('<div>').text(data).appendTo('body').fadeIn(200);
            });
            jsonPost("rpc/service/complex", d, function (data) {
                $('<div>').text(data).appendTo('body').fadeIn(200);
            });
        });
    </script>
</head>
<body>
<h2>Hi</h2>
</body>
</html>
