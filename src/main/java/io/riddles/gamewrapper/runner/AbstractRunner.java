// Copyright 2016 riddles.io (developers@riddles.io)

//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at

//        http://www.apache.org/licenses/LICENSE-2.0

//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//    For the full copyright and license information, please view the LICENSE
//    file that was distributed with this source code.

package io.riddles.gamewrapper.runner;

import io.riddles.gamewrapper.io.IOEngine;
import io.riddles.gamewrapper.io.IOPlayer;
import org.json.JSONObject;

import java.io.IOException;

/**
 * @author Niko van Meurs <niko@riddles.io>, Jim van Eeden <jim@riddles.io>
 */
public class AbstractRunner implements Reportable {

    private Long timebankMax;
    private Long timePerMove;
    private int maxTimeouts;
    private JSONObject results;

    public AbstractRunner(Long timebankMax, Long timePerMove, int maxTimeouts) {
        this.timebankMax = timebankMax;
        this.timePerMove = timePerMove;
        this.maxTimeouts = maxTimeouts;

        this.results = new JSONObject();
    }

    protected IOPlayer createPlayer(String command, int id) throws IOException {
        IOPlayer player = new IOPlayer(
                wrapCommand(command), id, this.timebankMax, this.timePerMove, this.maxTimeouts);
        player.run();

        return player;
    }

    protected IOEngine createEngine(String command, JSONObject engineConfig) throws IOException {
        IOEngine engine = new IOEngine(wrapCommand(command), engineConfig);
        engine.run();

        return engine;
    }

    protected void setResults(JSONObject value) {
        this.results = value;
    }

    /**
     * Execute command string as a process
     * @param command Command to start process
     * @return The started processs
     * @throws IOException exception
     */
    private Process wrapCommand(String command) throws IOException {
        System.out.println("executing: " + command);
        return Runtime.getRuntime().exec(command);
    }

    @Override
    public JSONObject getResults() {
        return this.results;
    }
}
