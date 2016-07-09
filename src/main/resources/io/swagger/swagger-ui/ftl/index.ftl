<!DOCTYPE html>
<html>
<#assign swaggerTitle = request.getAttribute('swaggerTitle')!/>
<#assign swaggerHead = request.getAttribute('swaggerHead')!/>
<#assign swaggerHeader = request.getAttribute('swaggerHeader')!/>
<#assign swaggerFooter = request.getAttribute('swaggerFooter')!/>
<head>
    <meta charset="UTF-8">
    <title>${swaggerTitle!'API'}</title>
<#include 'head.ftl'>
<#if swaggerHead?has_content><#include swaggerHead/></#if>
</head>
<#if swaggerHeader?has_content><#include swaggerHeader/></#if>
<body class="swagger-section">
<div id="message-bar" class="swagger-ui-wrap" data-sw-translate>&nbsp;</div>
<div id="swagger-ui-container" class="swagger-ui-wrap"></div>
<#if swaggerFooter?has_content><#include swaggerFooter/></#if>
</body>
</html>