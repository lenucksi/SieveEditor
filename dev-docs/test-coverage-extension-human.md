fuzz testing für die jvm gibts das hier <https://github.com/CodeIntelligenceTesting/jazzer> -> müssen tests für geschrieben werden

coverage hier und besonders im library plugin hoch kriegen und codecoveragen

<https://github.com/pre-commit/pre-commit> -> für markdown, java, shell usw
<https://gist.github.com/MangaD/6a85ee73dd19c833270524269159ed6e>
<https://github.com/pre-commit/action> gibts auch als direkte ci integration

<https://github.com/rvben/rumdl> -> markdown linter in github cis rein, in die skills für markdown rein, in die pre-commits rein -> pre-commit bau und setup skil, soll dieses pre-commit framework nutzen

- repo: <https://github.com/rvben/rumdl-pre-commit>
    rev: v0.0.177  # Use the latest release tag
    hooks:

  - id: rumdl

        # To only check (default):

        # args: []

        # To automatically fix issues:

        # args: [--fix]

<https://github.com/pre-commit/pre-commit-hooks> vielleicht auch interessante hooks bei
