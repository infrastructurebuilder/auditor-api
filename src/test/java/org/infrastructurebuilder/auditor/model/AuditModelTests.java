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
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import org.infrastructurebuilder.auditor.model.io.xpp3.AuditorResultsModelXpp3Reader;
import org.infrastructurebuilder.auditor.model.io.xpp3.AuditorResultsModelXpp3Writer;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.config.WorkingPathSupplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AuditModelTests {
  private WorkingPathSupplier wps;
  private Path path;

  @Before
  public void setUp() throws Exception {
    wps = new WorkingPathSupplier();
    Path source = wps.getRoot().resolve("test-classes").resolve("testFile.xml");
    path = wps.get().resolve(UUID.randomUUID().toString());
    IBUtils.copy(source, path);
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

    Path p = wps.getRoot().resolve("temp.xml");
    AuditorResultsModelXpp3Writer w = new AuditorResultsModelXpp3Writer();
    w.setFileComment("This is a test file!");
    w.write(new FileOutputStream(p.toString()), results);

    AuditorResults fromRead = new AuditorResultsModelXpp3Reader().read(new FileInputStream(p.toString()));
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

  @Test
  public void confirmReads() throws Exception {
    AuditorResults results = new AuditorResultsModelXpp3Reader().read(new FileInputStream(path.toString()));

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

  @Test
  public void testNonFileIO() throws Exception {
    StringWriter sw = new StringWriter();
    AuditorResults fileResults = new AuditorResultsModelXpp3Reader().read(new FileInputStream(path.toString()));
    new AuditorResultsModelXpp3Writer().write(sw, fileResults);

  }
}