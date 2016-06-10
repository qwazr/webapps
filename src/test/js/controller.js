response.contentType = "text/html"
response.writer.println('<html><body>')
response.writer.println('<h1>Test QWAZR Webapp</h1>')
if (request.parameters.testParam)
    response.writer.println('<p>testParam=' + request.parameters.testParam[0] + '</p>')
response.writer.println('</body></html>')