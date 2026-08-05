package forge;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * Regression test for a CI fix that pinned {@code mockito-core} to {@code 3.12.4} in
 * forge-gui-desktop/pom.xml. PowerMock 2.0.9's bridge to Mockito was broken by Mockito 5.x
 * switching to ByteBuddy-only inline mocking, and PowerMock cannot be dropped because it is
 * required to suppress static initialization of {@code ForgeConstants} (see
 * {@code forge.card.CardMockTestCase}). This test guards against an accidental future bump
 * of mockito-core to an incompatible version.
 */
public class MockitoDependencyVersionTest {

    private static final String EXPECTED_MOCKITO_CORE_VERSION = "3.12.4";

    @Test
    public void testMockitoCoreVersionIsPinnedForPowerMockCompatibility() throws Exception {
        File pomFile = locatePomXml();
        assertNotNull(pomFile, "Could not locate forge-gui-desktop/pom.xml to verify the mockito-core version");

        Document pom = parsePom(pomFile);
        String mockitoVersion = findDependencyVersion(pom, "org.mockito", "mockito-core");

        assertNotNull(mockitoVersion, "mockito-core dependency not found in forge-gui-desktop/pom.xml");
        assertEquals(mockitoVersion, EXPECTED_MOCKITO_CORE_VERSION,
                "mockito-core must stay on " + EXPECTED_MOCKITO_CORE_VERSION
                        + " for compatibility with powermock-api-mockito2 2.0.9; Mockito 5.x's "
                        + "ByteBuddy-only mocking breaks PowerMock's bridge to Mockito");
    }

    @Test
    public void testMockitoCoreVersionIsNotAnIncompatibleFiveXRelease() throws Exception {
        File pomFile = locatePomXml();
        assertNotNull(pomFile, "Could not locate forge-gui-desktop/pom.xml to verify the mockito-core version");

        Document pom = parsePom(pomFile);
        String mockitoVersion = findDependencyVersion(pom, "org.mockito", "mockito-core");

        assertNotNull(mockitoVersion, "mockito-core dependency not found in forge-gui-desktop/pom.xml");
        assertFalse(mockitoVersion.startsWith("5."),
                "mockito-core must not be a 5.x release; PowerMock 2.0.9 is incompatible with it. Found: "
                        + mockitoVersion);
    }

    private File locatePomXml() {
        String[] candidates = { "pom.xml", "forge-gui-desktop/pom.xml", "../forge-gui-desktop/pom.xml" };
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    private Document parsePom(File pomFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(pomFile);
    }

    private String findDependencyVersion(Document pom, String groupId, String artifactId) {
        NodeList dependencyNodes = pom.getElementsByTagName("dependency");
        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            Element dependency = (Element) dependencyNodes.item(i);
            if (groupId.equals(getDirectChildText(dependency, "groupId"))
                    && artifactId.equals(getDirectChildText(dependency, "artifactId"))) {
                return getDirectChildText(dependency, "version");
            }
        }
        return null;
    }

    private String getDirectChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(node.getNodeName())) {
                return node.getTextContent().trim();
            }
        }
        return null;
    }
}