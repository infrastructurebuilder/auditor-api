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

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.UUID;

import org.infrastructurebuilder.auditor.model.io.xpp3.AuditorResultsModelXpp3Reader;
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
  public void confirmReads() throws Exception {
    AuditorResults results = new AuditorResultsModelXpp3Reader().read(new FileInputStream(path.toString()));

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
  }
}