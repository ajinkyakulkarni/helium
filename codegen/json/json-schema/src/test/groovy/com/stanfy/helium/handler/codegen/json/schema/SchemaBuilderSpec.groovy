package com.stanfy.helium.handler.codegen.json.schema

import com.stanfy.helium.DefaultType
import com.stanfy.helium.model.Message
import com.stanfy.helium.model.Sequence
import com.stanfy.helium.model.Type
import spock.lang.Specification

class SchemaBuilderSpec extends Specification {

  private SchemaBuilder builder

  void setup() {
    builder = new SchemaBuilder()
  }

  def "should translate Helium data type into the correspondent JSON schema data type"() {
    expect:
    builder.translateType(new Type(name: heliumType.getLangName())) == jsonType

    where:
    heliumType         | jsonType
    DefaultType.BOOL   | JsonType.BOOLEAN
    DefaultType.BYTES  | JsonType.STRING
    DefaultType.DOUBLE | JsonType.NUMBER
    DefaultType.FLOAT  | JsonType.NUMBER
    DefaultType.INT32  | JsonType.INTEGER
    DefaultType.INT64  | JsonType.INTEGER
    DefaultType.STRING | JsonType.STRING
  }

  def "should translate complex types into object"() {
    setup:
    def msg = new Message(name: "ComplexType")
    def list = new Sequence()

    expect:
    builder.translateType(msg) == JsonType.OBJECT
    builder.translateType(list) != JsonType.OBJECT
  }

  def "should translate sequences into arrays"() {
    setup:
    def list = new Sequence()
    def msg = new Message(name: "ComplexType")

    expect:
    builder.translateType(list) == JsonType.ARRAY
    builder.translateType(msg) != JsonType.ARRAY
  }

  def "should propagate type descriptions"() {
    given:
    def type = new Type(name: "double", description: "bla bla")
    expect:
    builder.makeSchemaFromType(type).description == "bla bla"
  }

}
