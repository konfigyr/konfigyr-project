# konfigyr-mail

Gradle module that registers the [Konfigyr Mail SDK](github.com/konfigyr/konfigyr-mail) with following implementations:
 - Java SMTP Mail sender -> `konfigyr-mail-smtp`
 - Thymeleaf template engine -> `konfigyr-mail-thymeleaf`

### Layout template

This modules registers the common Thymleaf email layout template that can, and should, be used for
all mail message templates.

Use the [Thymeleaf Layout Dialect](https://github.com/ultraq/thymeleaf-layout-dialect) to include it
in the template like so:

```html
<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{mail/layout}"
      lang="en">
<head>
    <title>Your email title</title>
</head>
<body>
<header layout:fragment="header">
    <h1>Your email title</h1>
</header>
<main layout:fragment="main">
    <p>Mail message contents</p>
</main>
</body>
</html>
```
