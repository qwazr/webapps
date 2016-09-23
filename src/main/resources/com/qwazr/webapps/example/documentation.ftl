<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Documentation</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
          integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
</head>
<body>

<#if request.getAttribute("breadcrumb_parts")??>
<ol class="breadcrumb">
    <#assign path = "documentation">
    <li><a href="${request.contextPath}/${path}">Documentation</a></li>
    <#list request.getAttribute("breadcrumb_parts") as part>
        <#assign path = "${path}/${part?url}">
        <li><a href="${request.contextPath}/${path}">${part}</a></li>
    </#list>
</ol>
</#if>

<div class="container">

    <div class="row">
        <div class="col-md-9">
        <#if request.getAttribute("markdown")??>
            <div class="markdown-body">
            ${request.getAttribute("markdown")}
            </div>
        </#if>
        <#if request.getAttribute("adoc")??>
            <div class="adoc-body">
            ${request.getAttribute("adoc")}
            </div>
        </#if>
        </div>

        <div class="col-md-3">
            <br/><br/><br/>
        <#if request.getAttribute("filelist")??>
            <div class="file-list">
                <ul class="list-group">
                    <#list request.getAttribute("filelist") as file>
                        <#if !file.hidden>
                            <#if file != request.getAttribute("currentfile")>
                                <#if file.directory>
                                    <#if file.name != 'target'>
                                        <li class="list-group-item"><a href="${file.name?url}">${file.name}</a></li>
                                    </#if>
                                <#elseif file.name?ends_with('.md') || file.name?ends_with('.adoc')>
                                    <li class="list-group-item">
                                        <a href="${file.name?url}">${file.name?remove_ending('.md')?remove_ending('.adoc')?replace("_", " ")?capitalize}</a>
                                    </li>
                                </#if>
                            </#if>
                        </#if>
                    </#list>
                </ul>
            </div>
        </#if>
        </div>
    </div>
</div>

</body>
</html>