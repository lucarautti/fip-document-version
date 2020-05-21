# FIP Document Version

This Alfresco platform extension is an implementation of different parts, adapted to a specific use case.
Retrieve the latest and current version of a document and print it in the document’s content via a placeholder.
It also transform the docx to pdf, maintaining the original docx content unmodified.
Tested with **Alfresco Community Edition 5.2 **

## Alfresco Platform Extension
Starting from  [Alfresco Developer Guide](https://docs.alfresco.com/5.2/concepts/dev-for-developers.html) the logic core is a **custom action**, that you can recall where you want.
In this application, it has been recalled via Javascript in a specific task of a **custom workflow**.
Since it must work in the background there’s not a Share implementation or web script.

## docx4j

For docx traversing and content management, the docx4j library has been integrated.
After some tests the one that seems to work better is the **8.1.6 with reference implementations**.

[docx4j Maven repository](https://mvnrepository.com/artifact/org.docx4j/docx4j-JAXB-ReferenceImpl/8.1.6)
