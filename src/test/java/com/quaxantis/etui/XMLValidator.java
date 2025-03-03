package com.quaxantis.etui;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class XMLValidator {

    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    public XMLValidator() throws Exception {
    }

    @Test
    void validateTemplateCollection() throws Exception {
        validate(Path.of("C:\\Users\\Eigenaar\\.etui\\templates\\test.xml"),
                 Path.of("C:\\Users\\Eigenaar\\Projects\\etui\\src\\main\\resources\\com\\quaxantis\\etui\\template\\template-collection.xsd"));
    }

    @Test
    void validateTest0() throws Exception {
        validate(Path.of("C:\\Users\\Eigenaar\\Projects\\etui\\src\\test\\resources\\test0.xml"),
                 Path.of("C:\\Users\\Eigenaar\\Projects\\etui\\src\\test\\resources\\test0.xsd"));
    }

    @Test
    void validateTest1() throws Exception {
        validate(Path.of("C:\\Users\\Eigenaar\\Projects\\etui\\src\\test\\resources\\test1.xml"),
                 Path.of("C:\\Users\\Eigenaar\\Projects\\etui\\src\\test\\resources\\test1.xsd"));
    }

    void validate(Path xml, Path xsd) throws Exception {
        Validator validator = schemaFactory.newSchema(xsd.toFile()).newValidator();
        validator.validate(new StreamSource(new StringReader(Files.readString(xml))));
    }
}
