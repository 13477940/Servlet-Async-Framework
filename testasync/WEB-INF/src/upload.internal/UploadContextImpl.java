/*
 * Copyright (C) 2016 Adam Forgacs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 package upload.internal;

 import jakarta.servlet.http.HttpServletRequest;
 import upload.PartOutput;
 import upload.interfaces.PartStream;
 import upload.interfaces.UploadContext;

 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;

 /**
  * Default implementation of {@link UploadContext}.
  */
 public class UploadContextImpl implements UploadContext {

     /**
      * The request containing the bytes of the file. It's always a multipart, POST request.
      */
     private final HttpServletRequest request;
     /**
      * The user object. Only used by the callers, not necessary for these classes.
      */
     private final Object userObject;
     /**
      * The currently processed item.
      */
     private PartStreamImpl currentPart;
     /**
      * The active output.
      */
     private PartOutput output;
     /**
      * The list of the already processed items.
      */
     private final List<PartStream> partStreams = new ArrayList<>();
     /**
      * Determines whether the current item is buffering, that is, should new bytes be
      * stored in memory or written out the channel. It is set to false after the
      * part begin function is called.
      */
     private boolean buffering = true;
     /**
      * The total number for the bytes read for the current part.
      */
     private int partBytesRead;

     public UploadContextImpl(final HttpServletRequest request, final Object userObject) {
         this.request = request;
         this.userObject = userObject;
     }

     @Override
     public HttpServletRequest getRequest() {
         return request;
     }

     @Override
     public <T> T getUserObject(final Class<T> clazz) {
         return userObject == null ? null : clazz.cast(userObject);
     }

     @Override
     public PartStreamImpl getCurrentPart() {
         return currentPart;
     }

     @Override
     public PartOutput getCurrentOutput() {
         return output;
     }

     @Override
     public List<PartStream> getPartStreams() {
         return Collections.unmodifiableList(partStreams);
     }

     void reset(final PartStreamImpl newPart) {
         buffering = true;
         partBytesRead = 0;
         currentPart = newPart;
         partStreams.add(newPart);
         output = null;
     }

     void setOutput(final PartOutput output) {
         this.output = output;
         this.currentPart.setOutput(output);
     }

     boolean isBuffering() {
         return buffering;
     }

     void finishBuffering() {
         buffering = false;
     }

     void updatePartBytesRead() {
         currentPart.setSize(partBytesRead);
     }

     int getPartBytesRead() {
         return partBytesRead;
     }

     int incrementAndGetPartBytesRead(final int additional) {
         partBytesRead += additional;
         return partBytesRead;
     }
 }
