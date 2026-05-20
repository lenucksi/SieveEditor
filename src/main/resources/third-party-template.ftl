SieveEditor Third-Party Dependencies
=====================================

This project includes the following third-party dependencies,
each with its own license terms as listed below.

---
<#list licenseMap as entry>
<#assign artifact = entry.key>
<#assign licenses = entry.value>
<#list licenses as license>
  * ${artifact} -- ${license}
</#list>
</#list>
---

This product includes software developed by third parties.
For the full license texts, please refer to the individual
dependency JAR files in the Maven repository or visit the
project home pages listed in the project documentation.
