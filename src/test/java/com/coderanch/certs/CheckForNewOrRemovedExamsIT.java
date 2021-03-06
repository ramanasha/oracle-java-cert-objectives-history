package com.coderanch.certs;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.openqa.selenium.*;
import org.openqa.selenium.htmlunit.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Checks if any exams were added in the Java domain (or removed). If so, fail
 * test so know to add new text file to src/main/resources
 * 
 * @author jeanne
 *
 */
public class CheckForNewOrRemovedExamsIT {

	private static final String EXAM_LIST_URL = "http://education.oracle.com//pls/web_prod-plq-dad/ALL_EXAMS.getAllExamsWithProducts";

	private WebDriver driver;
	private Set<String> examNumbersFromOracle;
	private Set<String> examNumbersTested;

	// ----------------------------------------------------

	@BeforeEach
	public void connect() {
		driver = new HtmlUnitDriver();
	}

	// ----------------------------------------------------

	@Test
	public void listsInSync() throws Exception {
		setCurrentExamListFromOracle();
		setExamNumbersTested();

		Set<String> testedButRemovedFromOracleList = new HashSet<>(
				examNumbersTested);
		testedButRemovedFromOracleList.removeAll(examNumbersFromOracle);

		Set<String> onOracleListButNotTested = new HashSet<>(
				examNumbersFromOracle);
		onOracleListButNotTested.removeAll(examNumbersTested);
		
		testedButRemovedFromOracleList = normalizeBetaExamNumbers(testedButRemovedFromOracleList);
		onOracleListButNotTested = normalizeBetaExamNumbers(onOracleListButNotTested);

		assertEquals(0, testedButRemovedFromOracleList.size(), testedButRemovedFromOracleList
				+ "\nare checked for currency in this test project, "
				+ "\nbut no longer on Oracle's list. "
				+ "\nNext step: remove from enum so no longer check");

		assertEquals(0, onOracleListButNotTested.size(), onOracleListButNotTested
				+ "\nare on Oracle's list, but not tested in this project, "
				+ "\nNext step: add to enum so start checking");
	}

	/*
	 * normalize to change 1Z1 to 1Z0 for the exam prefix
	 */
	private Set<String> normalizeBetaExamNumbers(Set<String> testedButRemovedFromOracleList) {
		return testedButRemovedFromOracleList.stream().map(s -> s.replaceFirst("^1Z1", "1Z0"))
				.collect(Collectors.toSet());
	}

	private void setExamNumbersTested() {
		examNumbersTested = CertsToCheckEnum.getSetOfExamNumbers();
	}

	private void setCurrentExamListFromOracle() throws Exception {
		examNumbersFromOracle = new HashSet<>();
		driver.get(EXAM_LIST_URL);
		String source = driver.getPageSource();

		// currently called "Java" section in objectives
		String xpath = "//FAMILY[NAME[contains(text(), 'Java') and not(contains(text(), 'Cloud'))]]//NUMBER";
		NodeList nodeList = evaluateWithXpath(xpath, source);
		assertNotEquals(0, nodeList.getLength(), "no matching exams. maybe Oracle changed the XML format?");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node numberNode = nodeList.item(i);
			examNumbersFromOracle.add(numberNode.getTextContent().trim());
		}
	}

	private NodeList evaluateWithXpath(String expression, String source)
			throws ParserConfigurationException, SAXException, IOException,
			XPathExpressionException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder
				.parse(new ByteArrayInputStream(source.getBytes()));
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		return (NodeList) xpath.evaluate(expression, doc.getDocumentElement(),
				XPathConstants.NODESET);
	}
}
