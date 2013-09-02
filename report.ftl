<html>
  <head>
    <title>Titel (${time})</title>
  </head>
  <body>
    <h3>Titel (${time})</h3>
    <table border=1>
      <tr><th>Titel der Daten</th></tr>
      <#list data as value>
      <tr><td>${value}</td></tr>
      </#list>
    </table>
    <hr>
    (c) 2013 Copyright by arago Institut f&uuml;r komplexes Datenmanagement AG 
  </body>
</html> 
