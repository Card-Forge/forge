<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html lang="en">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
	<title>${title}</title>
	<style type="text/css">
  <!--
  body, html, img, a {margin: 0; padding: 0; border: none;}
  #back {display: none;padding: 5px; background-color: yellow;font-size: 150%;}
  @media screen { #back {display: block; position: absolute; right: 0; top: 0;} }
  -->
  </style>
</head>
<body>
<#list urls as url>
  <img src="${url}" height="319" width="222" style="margin: 0 1px 1px 0;">
</#list>  
</body>
</html>