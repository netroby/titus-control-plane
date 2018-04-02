/*
 * Copyright 2018 Netflix, Inc.
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

package io.netflix.titus.federation.service;

import io.grpc.stub.AbstractStub;
import io.netflix.titus.api.federation.model.Cell;

class CellResponse<STUB extends AbstractStub<STUB>, T> {
    private final Cell cell;
    private final STUB client;
    private final T result;

    CellResponse(Cell cell, STUB client, T result) {
        this.cell = cell;
        this.client = client;
        this.result = result;
    }

    public Cell getCell() {
        return cell;
    }

    public STUB getClient() {
        return client;
    }

    public T getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "CellResponse{" +
                "cell=" + cell +
                ", client=" + client +
                ", result=" + result +
                '}';
    }
}