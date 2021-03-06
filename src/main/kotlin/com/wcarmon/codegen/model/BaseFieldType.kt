package com.wcarmon.codegen.model

/**
 * Represents types for many popular languages
 * (java, kotlin, golang, rust, typescript, sql, c++ ...).
 *
 * Modifiers like nullable, generics, mutable are in [LogicalFieldType]
 */
enum class BaseFieldType {

  BOOLEAN,

  /**
   * Small enough to fit in memory
   *
   * PostgreSQL:
   * - bytea fits in memory
   * - ByteArray uses [java.sql.ResultSet.getBytes]
   *
   * - BLOBs are too big to fit in memory
   * - BLOBs are non-standard and more annoying to manage than file references
   * - BLOBs must be accessed in a transaction (autoCommit = false)
   * - BLOB uses [java.sql.ResultSet.getBlob] and [java.sql.Blob.getBinaryStream]
   * - BLOB uses [java.sql.PreparedStatement.setBinaryStream]
   */
  BYTE_ARRAY,

  // -- Characters
  CHAR, // 16-bit Unicode character
  COLOR,
  EMAIL,
  PATH,
  PHONE_NUMBER,
  STRING,
  URI,
  URL,
  UUID, // RFC 4122, ISO/IEC 9834-8:2005

  // -- Numeric
  FLOAT_32,
  FLOAT_64,
  FLOAT_BIG,

  /**
   * DB2:
   * Golang:
   * Java:
   * Kotlin:
   * Mysql/Maria:
   * Oracle:
   * SQLite:
   * Rust:        i128
   * PostgreSQL:  NUMERIC(20,0)
   */
  INT_128,

  /**
   * DB2:         SMALLINT
   * Golang:      int16
   * Java:        Short, short
   * Kotlin:      Short
   * Mysql/Maria: SMALLINT
   * Oracle:      SHORTINTEGER
   * PostgreSQL:  INT2, SMALLINT, NUMERIC(3,0)
   * Rust:        i16
   * SQLite:      INTEGER
   */
  INT_16,

  /**
   * DB2:           INTEGER, INT
   * Golang:        int32
   * Java:          Integer, int
   * Kotlin:        Int
   * Maria/Mysql:   INT
   * Oracle:        INTEGER
   * PostgreSQL:    INT4, INTEGER, NUMERIC(5,0)
   * Rust:          i32
   * SQLite:        INTEGER
   */
  INT_32,

  /**
   * DB2:         BIGINT
   * Golang:      int64
   * Java:        Long, long
   * Kotlin:      Long
   * Maria/Mysql: BIGINT
   * Oracle:      LONGINTEGER
   * PostgreSQL:  INT8, BIGINT, NUMERIC(10,0)
   * Rust:        i64
   * SQLite:      INTEGER
   */
  INT_64,

  /**
   * DB2:         --
   * Golang:      byte
   * Java:        Byte, byte
   * Kotlin:      Byte
   * Mysql/Maria: TINYINT
   * Oracle:      --
   * SQLite:      INTEGER
   * Rust:        i8
   * PostgreSQL:  NUMERIC(2,0)
   */
  INT_8,

  INT_BIG,

  // -- Temporal
  DURATION,               // measured in seconds & nanos
  MONTH_DAY,              // eg. birthdays
  PERIOD,                 // measured in years, months (day agnostic) or days (time agnostic)
  UTC_INSTANT,            // seconds + nanos since jan 1 1970
  UTC_TIME,               // eg. daily meeting time, market opening time (y/m/d agnostic)
  WEEK_OF_YEAR,
  YEAR,
  YEAR_MONTH,             // eg. credit card expiration
  ZONE_AGNOSTIC_DATE,     // eg. birthdate (tz agnostic)
  ZONE_AGNOSTIC_DATE_TIME, // eg. birthdate (tz agnostic)
  ZONE_AGNOSTIC_TIME,     // eg. store closing time (tz agnostic)
  ZONE_OFFSET,            // seconds (with upper bound)
  ZONED_DATE_TIME,        // Instant + offset + tz rules

  // -- Collections
  ARRAY,
  LIST,
  MAP,
  SET,

  USER_DEFINED
  ;

  companion object {

    /**
     * See [com.wcarmon.codegen.model.LogicalFieldTypeTest] for examples
     */
    @JvmStatic
    fun parse(value: String): BaseFieldType =
      MAPPINGS
        .entries
        .firstOrNull { entry ->
          entry.key.equals(value, true)
        }?.value
        ?: USER_DEFINED
//        ?: throw IllegalArgumentException("Failed to parse baseType for value='$value'")

    private val MAPPINGS = mapOf(
      "[]byte" to BYTE_ARRAY,
      "android.graphics.Color" to COLOR,
      "byte[]" to BYTE_ARRAY,
      "golang.bool" to BOOLEAN,
      "golang.byte" to INT_8,
      "golang.float32" to FLOAT_32,
      "golang.float64" to FLOAT_64,
      "golang.int16" to INT_16,
      "golang.int32" to INT_32,
      "golang.int64" to INT_64,
      "golang.int8" to INT_8,
      "golang.math.big.Float" to FLOAT_BIG,
      "golang.math.big.Int" to INT_BIG,
      "golang.rune" to INT_32,
      "golang.string" to STRING,
      "golang.time.Duration" to DURATION,
      "golang.time.Time" to UTC_INSTANT,
      "golang.uint16" to INT_16,
      "golang.uint32" to INT_32,
      "golang.uint64" to INT_64,
      "golang.uint8" to INT_16,
      "java.io.File" to PATH,
      "java.lang.Boolean" to BOOLEAN,
      "java.lang.Byte" to INT_8,
      "java.lang.char" to CHAR,
      "java.lang.Character" to CHAR,
      "java.lang.Double" to FLOAT_64,
      "java.lang.Float" to FLOAT_32,
      "java.lang.int" to INT_32,
      "java.lang.Integer" to INT_32,
      "java.lang.Long" to INT_64,
      "java.lang.Short" to INT_16,
      "java.lang.String" to STRING,
      "java.math.BigDecimal" to FLOAT_BIG,
      "java.math.BigInteger" to INT_BIG,
      "java.net.URI" to URI,
      "java.net.URL" to URL,
      "java.nio.file.Path" to PATH,
      "java.sql.Blob" to ARRAY,
      "java.sql.Date" to ZONE_AGNOSTIC_DATE,
      "java.sql.Time" to UTC_INSTANT,
      "java.sql.Timestamp" to UTC_INSTANT,
      "java.time.Duration" to DURATION,
      "java.time.Instant" to UTC_INSTANT,
      "java.time.LocalDate" to ZONE_AGNOSTIC_DATE,
      "java.time.LocalDateTime" to ZONE_AGNOSTIC_DATE_TIME,
      "java.time.LocalTime" to ZONE_AGNOSTIC_TIME,
      "java.time.MonthDay" to MONTH_DAY,
      "java.time.OffsetTime" to UTC_TIME,
      "java.time.Period" to PERIOD,
      "java.time.Year" to YEAR,
      "java.time.YearMonth" to YEAR_MONTH,
      "java.time.ZonedDateTime" to ZONED_DATE_TIME,
      "java.time.ZoneOffset" to ZONE_OFFSET,
      "java.util.Date" to UTC_INSTANT,
      "java.util.List" to LIST,
      "java.util.Map" to MAP,
      "java.util.Set" to SET,
      "java.util.UUID" to UUID,
      "javafx.scene.paint.Color" to COLOR,
      "kotlin.Byte" to INT_8,
      "kotlin.ByteArray" to BYTE_ARRAY,
      "kotlin.collections.List" to LIST,
      "kotlin.collections.Map" to MAP,
      "kotlin.collections.Set" to SET,
      "kotlin.Double" to FLOAT_64,
      "kotlin.Float" to FLOAT_32,
      "kotlin.Int" to INT_32,
      "kotlin.Long" to INT_64,
      "kotlin.Short" to INT_16,
      "postgres.bigint" to INT_64,
      "postgres.bigserial" to INT_64,
      "postgres.bool" to BOOLEAN,
      "postgres.boolean" to BOOLEAN,
      "postgres.bytea" to ARRAY,
      "postgres.bytea" to BYTE_ARRAY,
      "postgres.character" to STRING,
      "postgres.date" to ZONE_AGNOSTIC_DATE,
      "postgres.double precision" to FLOAT_64,
      "postgres.float4" to FLOAT_32,
      "postgres.float8" to FLOAT_64,
      "postgres.int" to INT_32,
      "postgres.int2" to INT_16,
      "postgres.int4" to INT_32,
      "postgres.int8" to INT_64,
      "postgres.integer" to INT_32,
      "postgres.json" to STRING,
      "postgres.jsonb" to ARRAY,
      "postgres.real" to FLOAT_32,
      "postgres.serial" to INT_32,
      "postgres.serial2" to INT_16,
      "postgres.serial4" to INT_32,
      "postgres.serial8" to INT_64,
      "postgres.smallint" to INT_16,
      "postgres.smallserial" to INT_16,
      "postgres.text" to STRING,
      "postgres.timestamp with time zone" to UTC_INSTANT,
      "postgres.timestamp without time zone" to ZONE_AGNOSTIC_DATE,
      "postgres.timestamptz" to UTC_INSTANT,
      "postgres.tsquery" to STRING,
      "postgres.uuid" to UUID,
      "postgres.varchar" to STRING,
      "postgres.xml" to STRING,
      "rust.bool" to BOOLEAN,
      "rust.ByteArray" to BYTE_ARRAY,
      "rust.f32" to FLOAT_32,
      "rust.f64" to FLOAT_64,
      "rust.i128" to INT_128,
      "rust.i16" to INT_16,
      "rust.i32" to INT_32,
      "rust.i64" to INT_64,
      "rust.i8" to INT_8,
      "rust.str" to STRING,
      "rust.String" to STRING,
      "rust.u128" to INT_128,
      "rust.u16" to INT_16,
      "rust.u32" to INT_32,
      "rust.u64" to INT_64,
      "rust.u8" to INT_8,

      // -- Convenient unambiguous aliases
      "array" to ARRAY,
      "bigfloat" to FLOAT_BIG,
      "bigint" to INT_BIG,
      "bool" to BOOLEAN,
      "boolean" to BOOLEAN,
      "bytearray" to BYTE_ARRAY,
      "character" to CHAR,
      "color" to COLOR,
      "datetime-local" to ZONE_AGNOSTIC_DATE_TIME,
      "dictionary" to MAP,
      "duration" to DURATION,
      "email" to EMAIL,
      "file" to PATH,
      "float32" to FLOAT_32,
      "float4" to FLOAT_32,
      "float64" to FLOAT_64,
      "float8" to FLOAT_64,
      "instant" to UTC_INSTANT,
      "int" to INT_32,  // java, kotlin, postgresql, mysql, oracle
      "int16" to INT_16,
      "int2" to INT_16,
      "int32" to INT_32,
      "int64" to INT_64,
      "integer" to INT_32,  // java, kotlin, postgresql, mysql, oracle
      "list" to LIST,
      "map" to MAP,
      "path" to PATH,
      "phoneNumber" to PHONE_NUMBER,
      "set" to SET,
      "smallint" to INT_16,
      "string" to STRING,
      "tel" to PHONE_NUMBER,
      "uint16" to INT_16,
      "uint32" to INT_32,
      "uint64" to INT_64,
      "uri" to URI,
      "url" to URL,
      "uuid" to UUID,
      "varchar" to STRING,
      "weekOfYear" to WEEK_OF_YEAR,


      // -- Not supported
      // golang.int             --  32-bit on 32 bit systems, 64-bit on 64 bit systems
      // golang.uint            -- 32-bit on 32 bit systems, 64-bit on 64 bit systems
      // java.time.OffsetDateTime -- offset is a presentation & date parsing concern
      // postgres.interval      -- it covers both INTERVAL and PERIOD
      // postgres.money         -- locale-sensitive (size depends on config)
      // postgres.time with time zone -- documentation discourages usage
      // postgres.timez         -- documentation discourages usage


      // -------------------------------------------
      // -- Decide on these:
      //TODO: "enum" to ,

      //TODO: "postgres.bit" to ,
      //TODO: "postgres.decimal" to FLOAT_64 or BIG_FLOAT,
      //TODO: "postgres.numeric" to FLOAT_64 or BIG_FLOAT or INT_64 or BIG_INT,
      //TODO: "postgres.varbit" to ,

      //TODO: "rust array" to ,
      //TODO: "rust slice" to ,
      //TODO: "rust.char" to ,

      // -- Network address
      //TODO: "postgres.cidr" to ,
      //TODO: "postgres.inet" to ,
      //TODO: "postgres.macaddr" to ,
      //TODO: "postgres.macaddr8" to ,

      // -- geometric
      //TODO: "postgres.box" to ,
      //TODO: "postgres.circle" to ,
      //TODO: "postgres.line" to ,
      //TODO: "postgres.lseg" to ,
      //TODO: "postgres.path" to ,
      //TODO: "postgres.point" to ,
      //TODO: "postgres.polygon" to ,

      //TODO: "java.time.ZoneId" to ,

      //TODO: mysql.*
      //TODO: maria.*

      //TODO: golang.complex128
      //TODO: golang.complex64
      //TODO: golang.time.Location

    ).toSortedMap(String.CASE_INSENSITIVE_ORDER)
  }

  val canHaveScale by lazy {
    when (this) {
      FLOAT_32,
      FLOAT_64,
      FLOAT_BIG,
      -> true
      else -> false
    }
  }

  //TODO: ambiguous for USER_DEFINED
  val isCollection by lazy {
    when (this) {
      ARRAY,
      LIST,
      MAP,
      SET,
      -> true

      else -> false
    }
  }

  val isTemporal by lazy {
    when (this) {
      DURATION,
      MONTH_DAY,
      PERIOD,
      UTC_INSTANT,
      UTC_TIME,
      YEAR,
      YEAR_MONTH,
      ZONE_AGNOSTIC_DATE,
      ZONE_AGNOSTIC_TIME,
      ZONE_OFFSET,
      ZONED_DATE_TIME,
      -> true

      else -> false
    }
  }

  val isNumeric by lazy {
    when (this) {
      FLOAT_32,
      FLOAT_64,
      FLOAT_BIG,
      INT_128,
      INT_16,
      INT_32,
      INT_64,
      INT_8,
      INT_BIG,
      YEAR,
      ZONE_OFFSET,
      -> true

      else -> false
    }
  }

  /**
   *
   * @return null when no reasonable default exists
   */
  fun defaultPrecision(): Int? {
    if (!canHavePrecision) {
      return null
    }

    //TODO: does unsigned/signed make a difference?
    return when (this) {
      INT_128 -> 39 // == BigInteger("F".repeat(32), 16).toString(10).length  for unsigned
      INT_16 -> Short.MAX_VALUE.toString(10).length
      INT_32 -> Int.MAX_VALUE.toString(10).length

      INT_64,
      UTC_INSTANT,
      YEAR,
      ZONE_AGNOSTIC_DATE_TIME,
      -> Long.MAX_VALUE.toString(10).length

      INT_8,
      WEEK_OF_YEAR,
      -> Byte.MAX_VALUE.toString(10).length

      else -> null
    }
  }

  val requiresPrecision by lazy {
    when (this) {
      FLOAT_32,
      FLOAT_64,
      FLOAT_BIG,
      INT_128,
      INT_16,
      INT_32,
      INT_64,
      INT_8,
      INT_BIG,
      -> true
      else -> false
    }
  }

  val requiredTypeParameterCount by lazy {
    when (this) {
      MAP -> 2
      ARRAY,
      LIST,
      SET,
      -> 1
      else -> 0
    }
  }

  val canHavePrecision by lazy {
    requiresPrecision || this == USER_DEFINED
  }

  val canAssignStringLiteral by lazy {
    when (this) {
      BOOLEAN,
      BYTE_ARRAY,
      CHAR,
      FLOAT_32,
      FLOAT_64,
      FLOAT_BIG,
      INT_128,
      INT_16,
      INT_32,
      INT_64,
      INT_8,
      INT_BIG,
      LIST,
      MAP,
      SET,
      YEAR,
      ZONE_OFFSET,
      -> false

      COLOR,
      EMAIL,
      DURATION,
      MONTH_DAY,
      PATH,
      PERIOD,
      PHONE_NUMBER,
      STRING,
      URI,
      URL,
      USER_DEFINED,
      UTC_INSTANT,
      UTC_TIME,
      UUID,
      WEEK_OF_YEAR,
      YEAR_MONTH,
      ZONE_AGNOSTIC_DATE,
      ZONE_AGNOSTIC_DATE_TIME,
      ZONE_AGNOSTIC_TIME,
      ZONED_DATE_TIME,
      -> true

      ARRAY,
      -> TODO("Decide if base type can receive string literal: $this")
    }
  }
}
