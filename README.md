# FIP Document Version

This Alfresco platform extension is an implementation of different parts, adapted to a specific use case.
Retrieve the latest and current version of a document and print it in the document’s content via a placeholder.

## Alfresco Platform Extension
Starting from  [Alfresco Developer Guide](https://docs.alfresco.com/5.2/concepts/dev-for-developers.html) the logic core is a **custom action**, that you can recall where you want.
In this application, it has been recalled via Javascript in a specific task of a **custom workflow**.
Since it must work in the background there’s not a Share implementation or web script.

## docx4j

For docx traversing and content management, the docx4j library has been integrated.
After some tests the one that seems to work better, in this case, is the **8.1.6 with reference implementations**.
You just need to add it to your pom.xml dependencies.
Instead of the standard docx4j VariableReplace, there's a method, searchAndReplace found on [Stack Overflow](https://stackoverflow.com/questions/20484722/docx4j-how-to-replace-placeholder-with-value).
Thanks, demotics2002