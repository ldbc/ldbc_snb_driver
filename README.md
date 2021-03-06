![LDBC Logo](ldbc-logo.png)

[![Build Status](https://circleci.com/gh/ldbc/ldbc_snb_driver.svg?style=svg)](https://circleci.com/gh/ldbc/ldbc_snb_driver)

:scroll: If you wish to cite the LDBC SNB, please refer to the [documentation repository](https://github.com/ldbc/ldbc_snb_docs#how-to-cite-ldbc-benchmarks).

This driver is being developed as part of the Linked Data Benchmark Council EU-funded research project and will be used to run the benchmark workloads developed and released by LDBC:

* [LDBC Project Website](http://ldbcouncil.org/)
* [LDBC Twitter Account](https://twitter.com/LDBCouncil)
* [LDBC Facebook Page](https://www.facebook.com/ldbcouncil/)

## Compatibility

The LDBC Social Network Benchmark suite is continuously maintained with improvements in the specification, the data generator, the driver, and the reference implementation.
To ensure that you are using compatible LDBC repositories, use the following table:

| project | v0.3.x | v0.4.x |
| ------- | ------ | ------ |
| [Documentation](https://github.com/ldbc/ldbc_snb_docs) | [`v0.3.3`](https://github.com/ldbc/ldbc_snb_docs/releases/tag/v0.3.3) | [`dev`](https://github.com/ldbc/ldbc_snb_docs/tree/dev) |
| [Datagen](https://github.com/ldbc/ldbc_snb_datagen) | [`v0.3.3`](https://github.com/ldbc/ldbc_snb_datagen/releases/tag/v0.3.3) | [`dev`](https://github.com/ldbc/ldbc_snb_datagen/tree/dev) |
| [Driver](https://github.com/ldbc/ldbc_snb_driver) | [`v0.3.3`](https://github.com/ldbc/ldbc_snb_driver/releases/tag/0.3.3) | [`dev`](https://github.com/ldbc/ldbc_snb_driver/tree/dev) |
| [Implementations](https://github.com/ldbc/ldbc_snb_implementations) | [`stable`](https://github.com/ldbc/ldbc_snb_implementations/tree/stable) | [`dev`](https://github.com/ldbc/ldbc_snb_implementations/tree/dev) |

The `stable` branches of the repositories correspond to the `v0.3.x`, and the `dev` branches correspond to the `v0.4.x` releases.

### User Guide

```bash
git clone https://github.com/ldbc/ldbc_snb_driver.git
cd ldbc_snb_driver
mvn clean package -DskipTests
```

To quickly test the driver try the "simpleworkload" that is shipped with it by doing the following:

```bash
java \
  -cp target/jeeves-standalone.jar com.ldbc.driver.Client \
  --driver_mode EXECUTE_WORKLOAD \
  --database com.ldbc.driver.workloads.simple.db.SimpleDb \
  -P target/classes/configuration/simple/simpleworkload.properties \
  -P target/classes/configuration/ldbc_driver_default.properties
```

For more information, please refer to the [Documentation](https://github.com/ldbc/ldbc_driver/wiki).
