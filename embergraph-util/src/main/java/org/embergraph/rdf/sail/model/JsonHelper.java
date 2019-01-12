/*
Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
Copyright (C) Embergraph contributors 2019. All rights reserved.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package org.embergraph.rdf.sail.model;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;

/*
* Helper class for Json Serialization of Model Objects
 *
 * @author beebs
 */
public class JsonHelper {

  public static void writeRunningQueryList(Writer w, List<RunningQuery> rQueries)
      throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final TypeFactory typeFactory = mapper.getTypeFactory();
    final ObjectWriter writer =
        mapper.writerWithType(
            typeFactory.constructCollectionType(
                List.class, org.embergraph.rdf.sail.model.RunningQuery.class));

    writer.writeValue(w, rQueries);
  }

  public static List<RunningQuery> readRunningQueryList(InputStream is)
      throws IOException {

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final TypeFactory typeFactory = mapper.getTypeFactory();
    final ObjectReader reader =
        mapper.reader(
            typeFactory.constructCollectionType(
                List.class, org.embergraph.rdf.sail.model.RunningQuery.class));

    // TODO: Change this when upgrading to a newer Jackson version
    return (List<RunningQuery>) reader.readValue(is);
  }
}
