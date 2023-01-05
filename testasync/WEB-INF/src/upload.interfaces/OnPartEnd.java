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

 package upload.interfaces;

 import upload.UploadParser;

 import java.io.IOException;

 /**
  * A functional interface. An implementation of it must be passed in the
  * {@link UploadParser#onPartEnd(OnPartEnd)} method to call it at the end of parsing for each part.
  *
  * <p>This function is called after every byte has been read for the given part. There will be
  * an attempt to close the current output object just before calling this. That means setting
  * this can be skipped if all you want to do is to close the provided channel or stream.</p>
  */
 @FunctionalInterface
 public interface OnPartEnd {

     /**
      * The consumer function to implement.
      * @param context The upload context
      * @throws IOException If an error occurred with the current channel
      */
     void onPartEnd(UploadContext context) throws IOException;

 }
