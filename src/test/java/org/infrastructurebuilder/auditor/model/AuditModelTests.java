/**
 * Copyright © 2019 admin (admin@infrastructurebuilder.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.infrastructurebuilder.auditor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.UUID;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.infrastructurebuilder.auditor.model.io.xpp3.AuditorResultsModelXpp3Reader;
import org.infrastructurebuilder.auditor.model.io.xpp3.AuditorResultsModelXpp3ReaderEx;
import org.infrastructurebuilder.auditor.model.io.xpp3.AuditorResultsModelXpp3Writer;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.config.TestingPathSupplier;
import org.infrastructurebuilder.util.config.WorkingPathSupplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AuditModelTests {
  private TestingPathSupplier wps = new TestingPathSupplier();
  private Path path;
  private AuditorResultsModelXpp3Reader resultsReader;
  private AuditorResultsModelXpp3ReaderEx resultsReaderEx;
  private AuditorResultsModelXpp3Writer resultsWriter;
  private Path badPath1, badPath2;
  private AuditorInputSource dsis;

  @Before
  public void setUp() throws Exception {
    path = wps.getTestClasses().resolve("testFile.xml");
    resultsReader = new AuditorResultsModelXpp3Reader();
    resultsWriter = new AuditorResultsModelXpp3Writer();
    badPath1 = wps.getTestClasses().resolve("testBadFile1.xml");
    badPath2 = wps.getTestClasses().resolve("testBadFile2.xml");
    resultsReaderEx = new AuditorResultsModelXpp3ReaderEx();
    dsis = new AuditorInputSource();

  }

  @After
  public void teardown() {
    wps.finalize();
  }

  @Test
  public void confirmWrites() throws Exception {
    AuditResult test = new AuditResult();
    String fakeDescription = "This description will get added, then removed.";
    String realDescription = "This description will stay in!";
    test.addDescription(fakeDescription);
    test.removeDescription(fakeDescription);
    test.addDescription(realDescription);
    test.setTimestampEnd(new java.util.Date());
    test.setTimestampStart(new java.util.Date());

    AuditResult test2 = new AuditResult();
    test2.setAuditFailure(true);
    test2.setErrored(true);
    test2.setReported(true);
    test2.setDescriptions(Arrays.asList(realDescription));

    AuditResult unused = new AuditResult();

    AuditorResults results = new AuditorResults();

    assertNotNull("Modello's gets are eerily typesafe, in places: Results", results.getResults());
    assertNotNull("Modello's gets are eerily typesafe, in places: Description Headers",
        results.getDescriptionHeaders());

    results.setTimestampEnd(new java.util.Date());
    results.setTimestampStart(new java.util.Date());

    results.addResult(test);
    results.addResult(test2);
    results.addResult(unused);
    results.removeResult(unused);

    results.addDescriptionHeader(realDescription);
    results.addDescriptionHeader(fakeDescription);
    results.removeDescriptionHeader(fakeDescription);

    Path p = wps.get().resolve("temp.xml");
    resultsWriter.setFileComment("This is a test file!");
    try (OutputStream outs = Files.newOutputStream(p)) {
      resultsWriter.write(outs, results);
    }
    try (InputStream ins = Files.newInputStream(p, StandardOpenOption.READ)) {
      AuditorResults fromRead = resultsReader.read(ins);
      fromRead.getResults().parallelStream().forEach(result -> {
        assertEquals("should have only one result header", 1, result.getDescriptions().size());
      });

      assertEquals("Should show 1 failures in list", 1,
          fromRead.getResults().parallelStream().filter(r -> r.isAuditFailure()).count());

      assertEquals("Should show 1 errors in list", 1,
          fromRead.getResults().parallelStream().filter(r -> r.isErrored()).count());

      assertEquals("Should be reporting exactly one result", 1,
          fromRead.getResults().parallelStream().filter(r -> r.isReported()).count());
    }
  }

  @Test
  public void confirmReads() throws Exception {
    // You still need to close your resources
    try (InputStream ins = Files.newInputStream(path)) {
      AuditorResults results = resultsReader.read(ins);
      assertEquals("Name should be Testing Audit", "Testing Audit", results.getName());

      int headers = results.getDescriptionHeaders().size();
      assertEquals("size of description headers should be 2", 2, headers);
      results.getResults().parallelStream().forEach(result -> {
        assertEquals("result descriptions should match length of headers", headers, result.getDescriptions().size());
      });

      assertEquals("Should show two failures in list", 2,
          results.getResults().parallelStream().filter(r -> r.isAuditFailure()).count());

      assertEquals("Should show one error in list", 1,
          results.getResults().parallelStream().filter(r -> r.isErrored()).count());

      assertEquals("Should be reporting all results", 3,
          results.getResults().parallelStream().filter(r -> r.isReported()).count());

      assertTrue("End time should be greater than start time",
          results.getTimestampEnd().compareTo(results.getTimestampStart()) > 0);
    }
  }

  @Test
  public void testNonFileIO() throws Exception {
    StringWriter sw = new StringWriter();
    try (InputStream ins = Files.newInputStream(path)) {
      AuditorResults fileResults = resultsReaderEx.read(ins, true, dsis);
      resultsWriter.write(sw, fileResults);
      assertTrue(sw.toString().contains("audit"));
    }

  }

  @Test(expected = XmlPullParserException.class)
  public void confirmFail1() throws Exception {
    try (InputStream ins = Files.newInputStream(badPath1)) {
      AuditorResults results = resultsReaderEx.read(ins, true, dsis);
      fail("This should not work");
    }
  }
  @Test(expected = XmlPullParserException.class)
  public void confirmFail2() throws Exception {
    try (InputStream ins = Files.newInputStream(badPath2)) {
      AuditorResults results = resultsReaderEx.read(ins, true, dsis);
      fail("This should not work");
    }
  }

}