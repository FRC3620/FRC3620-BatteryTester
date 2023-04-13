var ws = new ReconnectingWebSocket("ws://127.0.0.1:8080/battery");
ws.debug = true;
ws.onmessage = function (event) {
    var messageFromServer = event;
    $('#output').append('<p>Received!!!!!: '+messageFromServer+'</p>');
}

ws.onopen = function (event) {
}

ws.onclose = function (event) {
}