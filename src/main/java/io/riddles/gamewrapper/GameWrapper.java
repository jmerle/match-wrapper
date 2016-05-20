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


package io.riddles.gamewrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import io.riddles.gamewrapper.io.IOEngine;
import io.riddles.gamewrapper.io.IOPlayer;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * GameWrapper class
 * 
 * Runs all the processes needed for playing the game, namely
 * the engine and all bots. EngineAPI class to handle communication
 * between those processes
 * 
 * @author Sid Mijnders <sid@riddles.io>, Jim van Eeden <jim@riddles.io>
 */
public class GameWrapper {
    
    private IOEngine engine;
    private ArrayList<IOPlayer> players; // ArrayList containing player handlers
    private long timebankMax = 10000l; // 10 seconds default
    private long timePerMove = 500l; // 0,5 seconds default
    private int maxTimeouts = 2; // 2 timeouts default before shutdown
    private String resultFilePath;

    public GameWrapper() {
        this.engine = null;
        this.players = new ArrayList<IOPlayer>();
    }
    
    /**
     * Sets timebank settings for the bots
     * @param timebankMax Maximum time and starting time in timebank
     * @param timePerMove Time added to the timebank each move
     */
    private void parseSettings(String timebankMax, String timePerMove, String maxTimeouts, String resultFilePath) {
        long tbm = Long.parseLong(timebankMax);
        long tpm = Long.parseLong(timePerMove);
        int mto = Integer.parseInt(maxTimeouts);
        if (tbm > 0)
            this.timebankMax = tbm;
        if (tpm > 0)
            this.timePerMove = tpm;
        if (mto >= 0)
            this.maxTimeouts = mto;

        this.resultFilePath = resultFilePath;
    }

    /**
     * Creates and starts player (bot) process and adds them
     * to player list
     * @param command Command to start process
     * @throws IOException
     */
    private void addPlayer(String command) throws IOException {
        int id = this.players.size();
        IOPlayer player = new IOPlayer(wrapCommand(command), id, this.timebankMax, 
                this.timePerMove, this.maxTimeouts);
        this.players.add(player);
        player.run();
    }

    /**
     * Creates and starts engine process
     * @param command Command to start process
     * @throws IOException
     */
    private void setEngine(String command) throws IOException {
        this.engine = new IOEngine(wrapCommand(command));
        this.engine.run();
    }

    /**
     * Execute command string as a process
     * @param command Command to start process
     * @return The started processs
     * @throws IOException
     */
    private Process wrapCommand(String command) throws IOException {
        System.out.println("executing: " + command);
        return Runtime.getRuntime().exec(command);
    }
    
    /**
     * Starts the game
     */
    private void start() {
        
        System.out.println("Starting...");
        
        EngineAPI api = new EngineAPI(this.engine, this.players);

        try {
            api.run();
        } catch (IOException ioex) {
            ioex.printStackTrace();
            return;
        }
        
        System.out.println("Saving game...");
        saveGame(api.askGameDetails(), api.askPlayedGame());

        System.out.println("Stopping...");
        stop();
    }

    private void printGame() {
        System.out.println("Bot data:");
        for (IOPlayer bot : this.players) {
            System.out.println(bot.getDump());
            System.out.println(bot.getStdout());
            System.out.println(bot.getStderr());
        }
        System.out.println("Engine data:");
        System.out.println(this.engine.getStdout());
        System.out.println(this.engine.getStderr());
    }
    
    private void saveGame(String details, String playedGame) {

        JSONObject output = new JSONObject();
        JSONArray players = new JSONArray();

        for (IOPlayer player : this.players) {

            String log    = player.getDump();
            String errors = player.getStderr();

            JSONObject playerOutput = new JSONObject();
            playerOutput.put("log", log);
            playerOutput.put("errors", errors);

            players.put(playerOutput);
        }

        output.put("details", details);
        output.put("game", playedGame);
        output.put("players", players);

        try {
            System.out.println("Writing to result.json");

            FileWriter writer = new FileWriter(resultFilePath);
            writer.write(output.toString());
            writer.close();

            System.out.println("Finished writing to result.json");

        } catch (IOException e) {
            System.err.println("Failed to write to result.json");
            System.err.println(e.getMessage());
        }
    }

    /**
     * Stops the game and all processes
     */
    private void stop() {

        // testing
//        this.printGame();

        System.out.println(this.engine.getStderr());

        for (IOPlayer bot : this.players) {
            bot.finish();
        }
        this.engine.finish();
        
        System.out.println("Done.");
        System.exit(0);
    }

    public static void main(String[] args) {

//         for (String arg : args) System.out.println(arg);

        GameWrapper game = new GameWrapper();
        
        try {
            game.parseSettings(args[0], args[1], args[2], args[3]);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Correct arguments not provided, failed to parse settings.");
        }

        try {
            game.setEngine(args[4]);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start engine.");
        }

        try {
            for (int i = 5; i < args.length; i++) {
                game.addPlayer(args[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start bot.");
        }
        
        try {
            game.start();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while running game.");
        }
    }
}