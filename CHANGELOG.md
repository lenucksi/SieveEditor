# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.3](https://github.com/lenucksi/SieveEditor/compare/v1.0.4...v1.0.3) (2026-02-14)


### ⚠ BREAKING CHANGES

* Existing encrypted passwords cannot be decrypted with new key. Users must re-enter passwords after upgrade.
* Simplified project from multi-module (parent + app) to single-module Maven structure. ManageSieveJ is now consumed via JitPack dependency rather than git submodule, eliminating the need for reactor builds.

### security

* replace hardcoded encryption key with machine-specific derivation (Phase 2) ([ba3d15e](https://github.com/lenucksi/SieveEditor/commit/ba3d15e485960d0725f3302e3fdf48b4619b3260))


### Features

* **build:** add local Flatpak build infrastructure and dependency docs ([0e39256](https://github.com/lenucksi/SieveEditor/commit/0e39256883b6100c282f0b20d85fe94d614406f6))
* **cli:** add command-line parameters for verbose logging and backend selection ([caa8c2a](https://github.com/lenucksi/SieveEditor/commit/caa8c2ad9aaf8ee85546501cb7deace848646561))
* **connection:** add auto-reconnect and keep-alive mechanism ([9fda350](https://github.com/lenucksi/SieveEditor/commit/9fda3505126d52c7d554a951ca88219d5ad6da15))
* **credentials:** implement pluggable credential storage system ([0484617](https://github.com/lenucksi/SieveEditor/commit/04846174a5341802d8dd0d309facfe06eb5d2f28))
* **file:** add local file load/save functionality ([6da6cf8](https://github.com/lenucksi/SieveEditor/commit/6da6cf8a45a35bd10e90397a493affa781409827))
* **flatpak:** add application screenshots to metainfo ([11a14ed](https://github.com/lenucksi/SieveEditor/commit/11a14ed937f231c743b3e8ac4712267b09e5e614))
* **flatpak:** add D-Bus Secret Service permission and XDG paths ([09a1746](https://github.com/lenucksi/SieveEditor/commit/09a1746ff271d1a3e9abb569c49b0b5fc1cefa76))
* **launcher:** add auto-build and improve sieveeditor.sh ([b6bb0eb](https://github.com/lenucksi/SieveEditor/commit/b6bb0eb31391902356f0b8f46b6ac0f1add9a125))
* **navigation:** add Sieve script structure navigator with rule detection ([80459f8](https://github.com/lenucksi/SieveEditor/commit/80459f8bbc017faa1100d987ace50d5d48c52b96))
* **parser:** add severity levels and multi-occurrence tracking for warnings ([4836db3](https://github.com/lenucksi/SieveEditor/commit/4836db36c0877aa3ab235a919fe922bc570c0ff7))
* **profiles:** add profile deletion functionality ([ea12a69](https://github.com/lenucksi/SieveEditor/commit/ea12a697b6a09109ec0e60c829c19c36186f0863))
* **security:** add interactive certificate trust dialog with fingerprint verification ([850e257](https://github.com/lenucksi/SieveEditor/commit/850e25719fb0def21d939523716b35edc3fc837c))
* **templates:** add template insertion for common Sieve patterns ([66754cd](https://github.com/lenucksi/SieveEditor/commit/66754cdbb3b9d15b6ae1bf8ac1b7df4da29a4b1a))
* **ui:** add auto-regeneration of navigator on comment edit ([d282d2c](https://github.com/lenucksi/SieveEditor/commit/d282d2c554c1c6c0a24cde0c505bfce702cac2de))
* **ui:** add clickable warnings list in navigator ([3f08e15](https://github.com/lenucksi/SieveEditor/commit/3f08e150ff58e5ac0e12a1ac37d8e75e501c06b8))
* **ui:** add ESC key handling to search panel ([65818b4](https://github.com/lenucksi/SieveEditor/commit/65818b490023d355020c654c6db5c8ed751fa127))
* **ui:** add full CRUD operations to Manage Scripts dialog ([7f76289](https://github.com/lenucksi/SieveEditor/commit/7f762893cf63973fafe944c71ddbb228e52ca825))
* **ui:** add integrated search and replace panel ([9d1ae71](https://github.com/lenucksi/SieveEditor/commit/9d1ae718919859eb9e1c2165b5b36161a3fbe250))
* **ui:** add Linux desktop integration and remove obsolete 4K workarounds ([e710bbe](https://github.com/lenucksi/SieveEditor/commit/e710bbe7d0f4cb05853591c37dad7dc064c55520))
* **ui:** add resizable warning panel with auto-sizing ([406a893](https://github.com/lenucksi/SieveEditor/commit/406a893d20b7713ea99981f41a8110ff1c6b3d16))
* **ui:** auto-size navigator width based on content ([ecb4e23](https://github.com/lenucksi/SieveEditor/commit/ecb4e23f419bfe6986a15a208bc15da06856b4bd))
* **ui:** improve navigator layout and visibility ([a0e121f](https://github.com/lenucksi/SieveEditor/commit/a0e121fa220c63d277bf8fa47cbd356474dfbe8a))
* **ui:** increase line number font size and remove redundant Load menu item ([a42ab14](https://github.com/lenucksi/SieveEditor/commit/a42ab147a6bf13957e8ac469ac0e74643597b07e))
* **ui:** integrate FlatLaf for modern look-and-feel and HiDPI support ([943dad4](https://github.com/lenucksi/SieveEditor/commit/943dad43ba9818bb04917df5519c4fd6e5a69e60))
* **validation:** add error navigation with jump-to-line functionality ([c4a1bca](https://github.com/lenucksi/SieveEditor/commit/c4a1bcab19b0ec75b52cd8bb9e60ebb8f59117d0))
* **validation:** add pre-save validation with user confirmation ([412dfc6](https://github.com/lenucksi/SieveEditor/commit/412dfc63a04fdb6ed0b07a422635b4fee624fb69))


### Bug Fixes

* add flathub remote repository configuration for Flatpak builds ([996a9b0](https://github.com/lenucksi/SieveEditor/commit/996a9b0a415ffe78128650587f004566a7f18e56))
* add Java 21 setup to CodeQL workflow and document default setup conflict ([82283a8](https://github.com/lenucksi/SieveEditor/commit/82283a87eb27a0c7b2d55f1a8db160c100d9d0cb))
* Add UnknownHostException to the multi-catch clause. ([45af2af](https://github.com/lenucksi/SieveEditor/commit/45af2af013dcaaf5da6a2c33f223bca414dfa6f1))
* **backends:** Deactivate all but the manual entry ([5caa523](https://github.com/lenucksi/SieveEditor/commit/5caa5237e6b09447a7b7f45d8dd207a0d720db05))
* **ci:** cleanup GitHub workflows and unify release architecture ([e48c660](https://github.com/lenucksi/SieveEditor/commit/e48c6608a8a39113438077f0f45b50ca71ba1e04))
* **ci:** correct workflow paths and clean up GitHub Actions config ([2b600a0](https://github.com/lenucksi/SieveEditor/commit/2b600a07d5ff86338a801c9d0eff4ba54a2016a2))
* **ci:** enable autobuild mode for CodeQL Java analysis ([ee849c2](https://github.com/lenucksi/SieveEditor/commit/ee849c215a509427c20b555c24bb7eb1331b54b7))
* **ci:** enable autobuild mode for CodeQL Java analysis ([8ee3c42](https://github.com/lenucksi/SieveEditor/commit/8ee3c4299c938461ff57c50eda1f286f3f8744e5))
* **ci:** remove invalid package-name parameter from Release Please ([ce3a97a](https://github.com/lenucksi/SieveEditor/commit/ce3a97a4482022cb6b7375d6c1aa64187a234e4c))
* **ci:** remove invalid package-name parameter from Release Please ([42f4419](https://github.com/lenucksi/SieveEditor/commit/42f4419854eb6111dcabcda6daca1eadcb32d274))
* convert SVG icon to proper PNG format ([16d5980](https://github.com/lenucksi/SieveEditor/commit/16d59800f0927fc768fe1fefcd21d881087ac6c8))
* create /app/bin directory before writing launcher script ([0060171](https://github.com/lenucksi/SieveEditor/commit/006017140a2daa5fb3b104ac552e5c9441ea468f))
* create /app/bin directory before writing launcher script ([e0c874e](https://github.com/lenucksi/SieveEditor/commit/e0c874e31bb75811b47c388a40db87c56238526d))
* **credentials:** add retry logic for KeePassXC association delays ([5325a01](https://github.com/lenucksi/SieveEditor/commit/5325a01fff7ce2d10b63397dc2256a1ee7d86e58))
* **credentials:** check database lock status before KeePassXC association ([8651080](https://github.com/lenucksi/SieveEditor/commit/8651080b56a78eff58b747dff86597c4fe08ac98))
* **credentials:** persist KeePassXC association and allow first-run key generation ([83eb33c](https://github.com/lenucksi/SieveEditor/commit/83eb33c6f1dfb7c563a114d0cf53c965925107d1))
* **credentials:** prevent getLogins() from blocking on fresh association ([3ce83fa](https://github.com/lenucksi/SieveEditor/commit/3ce83fa014a543596f064f4aaae8d2b726a47869))
* **credentials:** return null when OS keychain entry doesn't exist on first run ([b08cf9b](https://github.com/lenucksi/SieveEditor/commit/b08cf9b2f5ef386aa86401e53b67f7993939ae55))
* **deps:** add SLF4J simple binding to resolve logging warnings ([fc3b037](https://github.com/lenucksi/SieveEditor/commit/fc3b0374993dc9cd468c17b11e2bbd762388253c))
* **deps:** update dependency com.fifesoft:rsyntaxtextarea to v3.6.1 ([d8de44b](https://github.com/lenucksi/SieveEditor/commit/d8de44bed362c779781fabe546c9eac82c186031))
* **deps:** update dependency com.fifesoft:rsyntaxtextarea to v3.6.1 ([33e5fec](https://github.com/lenucksi/SieveEditor/commit/33e5fec4ebff8c9f1812552a65adc48ca46b2b38))
* **deps:** update dependency com.formdev:flatlaf to v3.7 ([ef815b0](https://github.com/lenucksi/SieveEditor/commit/ef815b00c04095d51c3222218cee094d49baa1d2))
* **deps:** update dependency com.formdev:flatlaf to v3.7 ([eb83d9c](https://github.com/lenucksi/SieveEditor/commit/eb83d9c390d820757507f731028fc1f54a75df6c))
* **deps:** update dependency commons-codec:commons-codec to v1.21.0 ([89bc1fe](https://github.com/lenucksi/SieveEditor/commit/89bc1fe0ac49ef74a266830ee1f47dc765a01541))
* **deps:** update dependency commons-codec:commons-codec to v1.21.0 ([e7b7924](https://github.com/lenucksi/SieveEditor/commit/e7b792449cec9a8cc7f342d1a2790b7cd11fea6f))
* **deps:** update dependency org.purejava:keepassxc-proxy-access to v1.3.1 ([5a9fd4c](https://github.com/lenucksi/SieveEditor/commit/5a9fd4c75655f5a9da0685914c99f7d103e3a24d))
* **deps:** update dependency org.purejava:keepassxc-proxy-access to v1.3.1 ([f2ce8f1](https://github.com/lenucksi/SieveEditor/commit/f2ce8f1c176dc86b83e4d2586ec169d25911424f))
* ensure macOS DMG version uses only 1-3 numeric components ([eb95404](https://github.com/lenucksi/SieveEditor/commit/eb954044d464ff65f55542b86c80cddb04c1645a))
* **flatpak:** comment out unreachable screenshot URL in metainfo ([7ab7f65](https://github.com/lenucksi/SieveEditor/commit/7ab7f65212a39f5db9d1fd25f63ba6730bcd7f77))
* **flatpak:** correct Java runtime path and add IPC permission ([d501326](https://github.com/lenucksi/SieveEditor/commit/d501326bab0c1b98760ac4de58fb3ff519cee886))
* **merge:** Merge branch 'master' ([d1208c3](https://github.com/lenucksi/SieveEditor/commit/d1208c3b7a9ac88d53f18f95006b1ac7aa017c0b))
* optimize PNG icon with pngcrush for maximum compatibility ([ca1db00](https://github.com/lenucksi/SieveEditor/commit/ca1db008ba15f195dda641f800a0a223343e7e0e))
* **packaging:** JVM flags and new icons ([9045b62](https://github.com/lenucksi/SieveEditor/commit/9045b62bc699a6c5e73df3c6ec0ed72926a52352))
* **parser:** handle OX Flag field with content in rule comments ([2dedb59](https://github.com/lenucksi/SieveEditor/commit/2dedb5923aefa72864be62fb9a3dd33dfb30c40e))
* **parser:** improve rule parser robustness for edge cases ([a9539dc](https://github.com/lenucksi/SieveEditor/commit/a9539dc6d18dae1ae6e88ef6f1276262bdf85212))
* **pom.xml:** Update JaCoCo conf + SCM section ([7989ae5](https://github.com/lenucksi/SieveEditor/commit/7989ae5d229e3170d7c9904acea0bcb5f52c04b0))
* **precommit:** Adapt precommit config ([405b786](https://github.com/lenucksi/SieveEditor/commit/405b7865394b0a4c1264d4cfbb3831de1a930eb7))
* **precommit:** Autoupdate pre-commit ([4036e99](https://github.com/lenucksi/SieveEditor/commit/4036e99bc841f4ed3c0bf09cc17f64ef14fb98fa))
* regenerate PNG icon with ImageMagick for better compatibility ([a60889b](https://github.com/lenucksi/SieveEditor/commit/a60889b563dbd1b24f2527ef1a0aac3c19bd7c63))
* resolve GHA packaging issues and add release uploads ([a4b3cd0](https://github.com/lenucksi/SieveEditor/commit/a4b3cd020e59511b42ef32b1b9bc1bab5ef20819))
* **rumdl:** MD046 rumdl breaks codeblocks ([65ef2a9](https://github.com/lenucksi/SieveEditor/commit/65ef2a99dd19dbd8df58cef015bfca39bcd9b0cd))
* **security:** add encryption algorithm fallback for platform compatibility ([411fd4a](https://github.com/lenucksi/SieveEditor/commit/411fd4ae0e28c3dcb94b5c603519cb0a0eaafe03))
* **security:** add third-tier encryption fallback for maximum compatibility ([dfd077e](https://github.com/lenucksi/SieveEditor/commit/dfd077e8f2f9a2139ef9486522024f397c862d93))
* **security:** catch UnknownHostException in MAC address retrieval ([45af2af](https://github.com/lenucksi/SieveEditor/commit/45af2af013dcaaf5da6a2c33f223bca414dfa6f1))
* **security:** configure IV generator for AES encryption algorithms ([527d53e](https://github.com/lenucksi/SieveEditor/commit/527d53eda0ba491f9c5047077e6e49725a67d400))
* **security:** implement proper SSL certificate validation (Phase 2) ([1ede4f5](https://github.com/lenucksi/SieveEditor/commit/1ede4f5811b35e356e7fb437f763f856342042fe))
* **security:** mask password in connection dialog ([2b4fedd](https://github.com/lenucksi/SieveEditor/commit/2b4fedd7b1afd6ec339f084a06faaa7d58550364))
* **sonarqube:** Sonar integration fix ([2253c35](https://github.com/lenucksi/SieveEditor/commit/2253c35b356dc01213112353e348d42ca99bd1cb))
* **test:** improve test isolation for PropertiesSieveTest ([855e94b](https://github.com/lenucksi/SieveEditor/commit/855e94ba47b19f4291341e17b1fced3a29db8563))
* **tests:** Add explicit cleanup in PropertiesSieveTest for Windows ([0d63016](https://github.com/lenucksi/SieveEditor/commit/0d63016193874da4e0217d74daed3ad83c70c331))
* **tests:** Fix Windows test failures and JaCoCo coverage integration ([7a226b9](https://github.com/lenucksi/SieveEditor/commit/7a226b9cf81b6b1ed418f248df5d0582214e6095))
* **tests:** improve Windows test isolation in PropertiesSieveTest ([8bb911e](https://github.com/lenucksi/SieveEditor/commit/8bb911e527e4d6c756a129c2f97fd7106d2bd55f))
* **test:** update PropertiesSieveTest to use AppDirectoryService paths ([9f2166f](https://github.com/lenucksi/SieveEditor/commit/9f2166f711d8f828366a3e89578ad9978e251624))
* **ui:** auto-focus password field in master password dialog ([0d49da1](https://github.com/lenucksi/SieveEditor/commit/0d49da101cadfc80d2cc37d67498501c51ba2153))
* **ui:** catch InaccessibleObjectException in setLinuxAppName() ([0970e4a](https://github.com/lenucksi/SieveEditor/commit/0970e4af18c971f6ac38a86ca26e5471a7fed425))
* **ui:** clear navigator panel on disconnect ([f5c0a32](https://github.com/lenucksi/SieveEditor/commit/f5c0a3244adfdc3d9752b17bed26a0b80e71bb42))
* **ui:** correct initialization order in RuleNavigatorPanel ([e7d0c16](https://github.com/lenucksi/SieveEditor/commit/e7d0c16e135b948e9c3d59b4bfe6273cb8807fb9))
* **ui:** filter mouse button 4/5 events in script dialog ([b97e3d1](https://github.com/lenucksi/SieveEditor/commit/b97e3d1861e65ad0bc48372fe1a50bc0bb7c0032))
* **ui:** move Script Rules navigator to right side ([5666f28](https://github.com/lenucksi/SieveEditor/commit/5666f287cc6c74ce7eff5c3feca4d981a281d62b))
* **ui:** replace hardcoded dialog sizes with pack() for FlatLaf compatibility ([8cd14f6](https://github.com/lenucksi/SieveEditor/commit/8cd14f612253149b0632fcc1181edda618324439))
* **ui:** scroll selected rule to top of editor viewport ([7ed6d87](https://github.com/lenucksi/SieveEditor/commit/7ed6d874df419cb21d9b37e1ff0281d6396ae7ab))
* update Flatpak runtime to 24.08 and resolve GitHub Actions build permissions ([b2975c8](https://github.com/lenucksi/SieveEditor/commit/b2975c83d4c4847be79af44137d425f5653bfd56))
* update Flatpak runtime to 24.08 and resolve GitHub Actions build… ([163ac59](https://github.com/lenucksi/SieveEditor/commit/163ac59c974af09dc33c124ec28873e4dda13bf9))
* use --user flag for flatpak-builder to fix dependency installation ([26edc14](https://github.com/lenucksi/SieveEditor/commit/26edc143475e5ff0afe3823bc15fed8dde472f47))


### Dependencies

* **deps-dev:** bump org.apache.maven.plugins:maven-compiler-plugin ([5f0f6df](https://github.com/lenucksi/SieveEditor/commit/5f0f6df6dedbcdf6c7a30ec9731496892d8d73cf))
* **deps-dev:** bump org.assertj:assertj-core in /app ([1f3fddc](https://github.com/lenucksi/SieveEditor/commit/1f3fddcec357c75bcbfb3a068215415f91e6773f))
* **deps-dev:** bump org.junit.jupiter:junit-jupiter in /app ([5f52176](https://github.com/lenucksi/SieveEditor/commit/5f52176de60c97c4e2dd774008daae1c67a3706f))
* **deps-dev:** bump org.mockito:mockito-core in /app ([b7eb189](https://github.com/lenucksi/SieveEditor/commit/b7eb1893147e6ead77aa14e774ce961be8daa24c))
* **deps-dev:** bump org.mockito:mockito-junit-jupiter in /app ([5b9f58b](https://github.com/lenucksi/SieveEditor/commit/5b9f58be181a3e17eecf3070a96c769b4f5eef59))
* **deps:** bump com.fifesoft:rsyntaxtextarea from 3.6.0 to 3.6.1 ([a4d9e47](https://github.com/lenucksi/SieveEditor/commit/a4d9e473c876bddbbaf47c4ef0e137f28c1a97b5))
* **deps:** bump com.fifesoft:rsyntaxtextarea in /app ([77106f1](https://github.com/lenucksi/SieveEditor/commit/77106f1b0893859d0c85d9bc189015cd93f61b5e))
* **deps:** bump com.formdev:flatlaf from 3.5.4 to 3.6.2 ([2851e52](https://github.com/lenucksi/SieveEditor/commit/2851e525850bd20a7e81e3332017ea4ed7362c09))
* **deps:** bump commons-codec:commons-codec in /app ([0f8359d](https://github.com/lenucksi/SieveEditor/commit/0f8359da1e17ebb7364717df3b293a14bf0f8ba9))
* **deps:** bump org.apache.maven.plugins:maven-assembly-plugin ([eff1205](https://github.com/lenucksi/SieveEditor/commit/eff12059330effa7467345594362f2a3cb71b4e1))
* **deps:** bump org.apache.maven.plugins:maven-assembly-plugin in /app ([bf3d842](https://github.com/lenucksi/SieveEditor/commit/bf3d842a1ba3873156d9d66011e25ff74b8b4b7a))
* **deps:** bump org.apache.maven.plugins:maven-compiler-plugin in /app ([04ad7ea](https://github.com/lenucksi/SieveEditor/commit/04ad7eaad45f48aa1724434f679c6ec1a9bdbc76))
* **deps:** bump org.apache.maven.plugins:maven-surefire-plugin in /app ([6cf4edb](https://github.com/lenucksi/SieveEditor/commit/6cf4edb1196520232c800ec68a0aa42c27a0da7a))
* **deps:** bump org.jacoco:jacoco-maven-plugin from 0.8.13 to 0.8.14 ([2de20e8](https://github.com/lenucksi/SieveEditor/commit/2de20e8bf2654f68a11a6242366d0c94b6d9ae16))
* **deps:** bump org.purejava:keepassxc-proxy-access from 1.3.0 to 1.3.1 ([2f52388](https://github.com/lenucksi/SieveEditor/commit/2f52388900cb519d42c763a546232c50c7d6bbdb))
* **deps:** bump org.sonarsource.scanner.maven:sonar-maven-plugin ([f575337](https://github.com/lenucksi/SieveEditor/commit/f575337aff6261f4e6c164f78209e273335dfe5d))
* **flatpak:** update Maven dependencies ([11ce764](https://github.com/lenucksi/SieveEditor/commit/11ce764f828ce485c5c2b399c712f3b36a8b7dcc))
* **flatpak:** update Maven dependencies ([6b55ecb](https://github.com/lenucksi/SieveEditor/commit/6b55ecb2fe9c7aa6974a1ab5a4d520a056159f65))
* **flatpak:** update Maven dependencies ([ad83c76](https://github.com/lenucksi/SieveEditor/commit/ad83c7649bbeedd547e90d5e6e88c16e52127296))
* **flatpak:** update Maven dependencies ([0c55936](https://github.com/lenucksi/SieveEditor/commit/0c559364d902c5416401790bf58baac2bfea0d44))
* **flatpak:** update Maven dependencies ([a680ce6](https://github.com/lenucksi/SieveEditor/commit/a680ce6abc162f42607f22a1b2d7d5bcb27721b3))
* **flatpak:** update Maven dependencies ([9617e38](https://github.com/lenucksi/SieveEditor/commit/9617e384385d01e217bea4f20bc630ba1110676c))


### Documentation

* add comprehensive credential storage refactor documentation ([ec06f5c](https://github.com/lenucksi/SieveEditor/commit/ec06f5cc9e2bcb780a98ffbbb6fb25af4ff3d353))
* add comprehensive ManageSieveJ security fix plan and patches ([b8df9bf](https://github.com/lenucksi/SieveEditor/commit/b8df9bfdbcfcb91406ee836448ae7d6a51db686a))
* add comprehensive security fix merge plan ([6db3bf5](https://github.com/lenucksi/SieveEditor/commit/6db3bf5344defce5d679ffa40778fe1eee39184b))
* add comprehensive security fix merge plan ([72b94de](https://github.com/lenucksi/SieveEditor/commit/72b94dec7f80529f6adbb3ce60fa8127bd815648))
* add Flathub submission guide ([901bac1](https://github.com/lenucksi/SieveEditor/commit/901bac158a7e6ed61db3da40aa46451b54a9e327))
* add implementation notes to NEXT-FEATURES-PROMPT ([7fa2095](https://github.com/lenucksi/SieveEditor/commit/7fa20957d4cce1f5e5f41032cc0e592df03169e6))
* add PR description for infrastructure modernization ([5761994](https://github.com/lenucksi/SieveEditor/commit/5761994cc8c6b9d55fa5fdd12202fdb025fadeef))
* add PR title and comprehensive description ([c7043c2](https://github.com/lenucksi/SieveEditor/commit/c7043c202021a60aa59f3b8729944bd0dd04f498))
* add security documentation and migration guide ([49b361f](https://github.com/lenucksi/SieveEditor/commit/49b361fadeaee8ed9f48d6fe4dc97a75a5cd8f93))
* add task status and FlatLaf implementation report ([4f17760](https://github.com/lenucksi/SieveEditor/commit/4f177606e55fbc9c03b9bad7ccda8ed6f2cf8aa3))
* analyze multi-module to single-module Maven refactoring ([26d4605](https://github.com/lenucksi/SieveEditor/commit/26d46057ca5ffe477235e89122bfe8348a3e4b42))
* document JDK mouse button bug and workaround ([3069089](https://github.com/lenucksi/SieveEditor/commit/3069089f0d0c7800be2c23da6e13b0bd46054410))
* reorganize development documentation into dev-docs/ ([ef6b934](https://github.com/lenucksi/SieveEditor/commit/ef6b934f55574084b787ed7cbb586143a45976fb))
* simplify CLAUDE-Task.md with completed items ([0b44e88](https://github.com/lenucksi/SieveEditor/commit/0b44e883a58ebf10d7252fc849ee336b9bd4b4a9))
* **tests:** Add CI failures analysis with fixes ([922f6ac](https://github.com/lenucksi/SieveEditor/commit/922f6acd497199e9d20cae19fb35057051de9fb6))
* update bug status for KeePassXC association issues ([7dcd145](https://github.com/lenucksi/SieveEditor/commit/7dcd145be1c7d1019409bf4f9a5e5d0bd5a608c7))
* update documentation for local file and template features ([e44780d](https://github.com/lenucksi/SieveEditor/commit/e44780d51fa0312c9db704640c5adfc4629040c6))
* update paths for single-module Maven structure ([08553dc](https://github.com/lenucksi/SieveEditor/commit/08553dc460e97bc8b8c968d6f432c8cde297b363))
* update security docs with certificate trust dialog feature ([9298336](https://github.com/lenucksi/SieveEditor/commit/9298336b7a662e9ce201cf7190bf2c04ac203988))


### Miscellaneous Chores

* release 1.0.3 for flatpak ([64dc73d](https://github.com/lenucksi/SieveEditor/commit/64dc73d7c1de2e03ccda54e84f6ca213aaa257b7))
* **release:** release 1.0.1 ([8a10edf](https://github.com/lenucksi/SieveEditor/commit/8a10edf792df587b22656fd8881221e5fa708bc4))
* **release:** release 1.0.2 ([ed7d42a](https://github.com/lenucksi/SieveEditor/commit/ed7d42a7e276a4be8367bbfa9247c8a4f33c30da))


### Code Refactoring

* migrate from multi-module to single-module Maven structure ([2fdeffa](https://github.com/lenucksi/SieveEditor/commit/2fdeffa63436d3cd8cd7be267ac6b20f39206932))

## [1.0.4](https://github.com/lenucksi/SieveEditor/compare/v1.0.3...v1.0.4) (2025-12-11)


### Dependencies

* **flatpak:** update Maven dependencies ([9617e38](https://github.com/lenucksi/SieveEditor/commit/9617e384385d01e217bea4f20bc630ba1110676c))

## [1.0.3](https://github.com/lenucksi/SieveEditor/compare/v1.0.2...v1.0.3) (2025-12-10)


### Miscellaneous Chores

* release 1.0.3 for flatpak ([64dc73d](https://github.com/lenucksi/SieveEditor/commit/64dc73d7c1de2e03ccda54e84f6ca213aaa257b7))

## [1.0.2](https://github.com/lenucksi/SieveEditor/compare/v1.0.1...v1.0.2) (2025-12-04)


### Miscellaneous Chores

* **release:** release 1.0.2 ([ed7d42a](https://github.com/lenucksi/SieveEditor/commit/ed7d42a7e276a4be8367bbfa9247c8a4f33c30da))

## [1.0.1](https://github.com/lenucksi/SieveEditor/compare/v1.0.0...v1.0.1) (2025-12-04)


### Miscellaneous Chores

* **release:** release 1.0.1 ([8a10edf](https://github.com/lenucksi/SieveEditor/commit/8a10edf792df587b22656fd8881221e5fa708bc4))

## [1.0.0](https://github.com/lenucksi/SieveEditor/compare/0.9.2...v1.0.0) (2025-12-04)


### ⚠ BREAKING CHANGES

* Existing encrypted passwords cannot be decrypted with new key. Users must re-enter passwords after upgrade.

### security

* replace hardcoded encryption key with locally entered secret.


### Features

* **file:** add local file load/save functionality ([6da6cf8](https://github.com/lenucksi/SieveEditor/commit/6da6cf8a45a35bd10e90397a493affa781409827))
* **flatpak:** add application screenshots to metainfo ([11a14ed](https://github.com/lenucksi/SieveEditor/commit/11a14ed937f231c743b3e8ac4712267b09e5e614))
* **flatpak:** add D-Bus Secret Service permission and XDG paths ([09a1746](https://github.com/lenucksi/SieveEditor/commit/09a1746ff271d1a3e9abb569c49b0b5fc1cefa76))
* **launcher:** add auto-build and improve sieveeditor.sh ([b6bb0eb](https://github.com/lenucksi/SieveEditor/commit/b6bb0eb31391902356f0b8f46b6ac0f1add9a125))
* **profiles:** add profile deletion functionality ([ea12a69](https://github.com/lenucksi/SieveEditor/commit/ea12a697b6a09109ec0e60c829c19c36186f0863))
* **security:** add interactive certificate trust dialog with fingerprint verification ([850e257](https://github.com/lenucksi/SieveEditor/commit/850e25719fb0def21d939523716b35edc3fc837c))
* **templates:** add template insertion for common Sieve patterns ([66754cd](https://github.com/lenucksi/SieveEditor/commit/66754cdbb3b9d15b6ae1bf8ac1b7df4da29a4b1a))
* **ui:** add full CRUD operations to Manage Scripts dialog ([7f76289](https://github.com/lenucksi/SieveEditor/commit/7f762893cf63973fafe944c71ddbb228e52ca825))
* **ui:** add Linux desktop integration and remove obsolete 4K workarounds ([e710bbe](https://github.com/lenucksi/SieveEditor/commit/e710bbe7d0f4cb05853591c37dad7dc064c55520))
* **ui:** integrate FlatLaf for modern look-and-feel and HiDPI support ([943dad4](https://github.com/lenucksi/SieveEditor/commit/943dad43ba9818bb04917df5519c4fd6e5a69e60))

### Dependencies

* **deps:** bump com.formdev:flatlaf from 3.5.4 to 3.6.2 ([2851e52](https://github.com/lenucksi/SieveEditor/commit/2851e525850bd20a7e81e3332017ea4ed7362c09))

## [Unreleased]

### Added

- Multi-platform packaging support (JAR, DEB, RPM, MSI, DMG, Flatpak)
- Automated release workflow with Release Please
- SLSA Level 3 provenance attestations for all artifacts

## [0.0.1] - Initial Development

### Added

- Initial project structure
- ManageSieve client integration (ManageSieveJ 0.3.3)
- Swing-based GUI with RSyntaxTextArea for Sieve script editing
- Jasypt-based password encryption for profile storage
- JUnit 5 test framework with Mockito and AssertJ
- JaCoCo code coverage reporting
- Multi-module Maven build system

[Unreleased]: https://github.com/lenucksi/SieveEditor/compare/v0.0.1...HEAD
[0.0.1]: https://github.com/lenucksi/SieveEditor/releases/tag/v0.0.1
