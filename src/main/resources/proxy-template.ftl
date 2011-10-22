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
  <img src="${url}" height="${height}" width="${width}" style="margin: 0 ${cardBorder}px ${cardBorder}px 0;">
</#list>  
<br>
  <table title="Spirited Away" style="font-size:10px;" border=1 cellpadding="5" cellspacing="1">
  <tr>
  <td width="211">
  </td>
  <td width="211">
  </td>
  </tr>
  </table>     
</body>
</html>