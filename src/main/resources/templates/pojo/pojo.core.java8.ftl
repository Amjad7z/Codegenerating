package ${request.packageName.value};

${request.java8View.serializeImports(
  entity.java8View.importsForFields,
  request.extraJVMImports,
  request.jvmContextClass)}

/**
 * Immutable POJO
 * <p>
 * See ${request.prettyTemplateName}
 */
<#-- TODO: include class documentation when present-->
@JsonPropertyOrder(alphabetic = true)
@JsonDeserialize(builder = ${entity.name.upperCamel}.${entity.name.upperCamel}Builder.class)
public final class ${entity.name.upperCamel} {

  ${entity.java8View.fieldDeclarations}

  private ${entity.name.upperCamel}( ${entity.name.upperCamel}Builder builder ) {
    //TODO: Validation here

    <#list entity.sortedFields as field>
      <#if field.collection>
<#--          TODO: fix this line -->
<#--      this.${field.name.lowerCamel} = ${field.java8View.unmodifiableCollectionMethod}(builder.${field.name.lowerCamel});-->
      <#else>
      this.${field.name.lowerCamel} = builder.${field.name.lowerCamel};
      </#if>
    </#list>
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ${entity.name.upperCamel} that = (${entity.name.upperCamel}) o;
    <#list entity.sortedFields as field>
      <#if field?is_first>
      return ${field.java8View.equalityExpression("this", "that")}
      <#elseif field?is_last>
        && ${field.java8View.equalityExpression("this", "that")};
<#--      TODO: handle arrays:  Arrays.equals-->
      <#else>
        && ${field.java8View.equalityExpression("this", "that")}
      </#if>
    </#list>
  }

  @Override
  public int hashCode() {
    return Objects.hash(
    <#list entity.sortedFields as field>
      ${field.name.lowerCamel}<#if field?is_last>);<#else>,</#if>
    </#list>
  }

  <#list entity.sortedFields as field>
    public ${field.java8View.typeLiteral} get${field.name.upperCamel}() {
      return this.${field.name.lowerCamel};
    }

  </#list>

  @Override
  public String toString() {
    return new StringJoiner(", ", ${entity.name.upperCamel}.class.getSimpleName() + "[", "]")
    <#list entity.sortedFields as field>
      <#if field.shouldQuoteInString>
      .add("${field.name.lowerCamel}='" + ${field.name.lowerCamel} + "'")
      <#else>
      .add("${field.name.lowerCamel}=" + ${field.name.lowerCamel})
      </#if>
    </#list>
    .toString();
  }

  public static ${entity.name.upperCamel}Builder builder() {
    return new ${entity.name.upperCamel}Builder();
  }

  @JsonPOJOBuilder(withPrefix = "", buildMethodName = "build")
  public static class ${entity.name.upperCamel}Builder {
<#-- TODO: set default values on fields here-->
    <#list entity.sortedFields as field>
    private ${field.java8View.typeLiteral} ${field.name.lowerCamel};
    </#list>

    ${entity.name.upperCamel}Builder() {
    }

    <#list entity.sortedFields as field>
    public ${entity.name.upperCamel}Builder ${field.name.lowerCamel}(${field.java8View.typeLiteral} value) {
      this.${field.name.lowerCamel} = value;
      return this;
    }

      <#if field.collection>

        public ${entity.name.upperCamel}Builder ${field.name.lowerCamel}(${field.type.typeParameters[0]} value) {
          if (this.${field.name.lowerCamel} == null) {
            this.${field.name.lowerCamel} = new ${field.java8View.newCollectionExpression()};
          }

          this.${field.name.lowerCamel}.add(value);
          return this;
        }
      </#if>
    </#list>

    public ${entity.name.upperCamel} build() {
      return new ${entity.name.upperCamel}(this);
    }
  }
}
