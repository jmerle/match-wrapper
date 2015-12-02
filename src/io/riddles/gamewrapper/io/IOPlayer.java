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

package io.riddles.gamewrapper.io;

import java.io.IOException;

/**
 * IOPlayer class
 * 
 * Extends IOWrapper class by adding stuff specifically
 * for bot processes
 * 
 * @author Sid Mijnders <sid@riddles.io>, Jim van Eeden <jim@starapple.nl>
 */
public class IOPlayer extends IOWrapper {

    private StringBuilder dump;
    private int errorCounter;
    private long timebank;
    private long timebankMax;
    private long timePerMove;
    private int maxTimeouts;
    
    private final String NULL_MOVE = "no_moves";

    public IOPlayer(Process process, long timebankMax, long timePerMove, int maxTimeouts) {
        super(process);
        this.dump = new StringBuilder();
        this.errorCounter = 0;
        this.timebank = timebankMax;
        this.timebankMax = timebankMax;
        this.timePerMove = timePerMove;
        this.maxTimeouts = maxTimeouts;
    }
 
    /**
     * Send line to bot
     * @param line Line to send
     * @return True if write was successful, false otherwise
     */
    public void send(String line) {
        addToDump(line);
        if (!super.write(line) && !this.finished) {
        	addToDump("Write to bot failed, shutting down...");
        }
    }
    
    /**
     * Send line to bot and waits for response taking
     * the bot's timebank into account
     * @param line Line to output
     * @return Bot's response
     * @throws IOException
     */
    public String ask(String line) throws IOException {
    	send(String.format("%s %d", line, this.timebank));
    	return getResponse();
    }

    /**
     * Waits until bot returns a response and returns it
     * @return Bot's response, returns and empty string when there is no response
     */
    public String getResponse() {
        
        if (this.errorCounter > this.maxTimeouts) {
            addToDump(String.format("Maximum number (%d) of time-outs reached: skipping all moves.", this.maxTimeouts));
            return "";
        }

        long startTime = System.currentTimeMillis();
        
        String response = super.getResponse(this.timebank);
        
        long timeElapsed = System.currentTimeMillis() - startTime;
        updateTimeBank(timeElapsed);

        if(response.equalsIgnoreCase(this.NULL_MOVE)) {
            botDump(this.NULL_MOVE);
            return "";
        }
        if(response.isEmpty()) {
        	botDump("null");
        	return "";
        }

        botDump(response);
        return response;
    }

    /**
     * Handles everything when a bot response
     * times out
     * @param timeout Time before timeout
     * @return Empty string
     */
    protected String handleResponseTimeout(long timeout) {
        addToDump(String.format("Response timed out (%dms), let your bot return '%s'"
        	+ " instead of nothing or make it faster.", timeout, this.NULL_MOVE));
        addError();
        return "";
    }
    
    /**
     * Increases error counter, call this method
     * when a write fails or when there is no
     * response
     */
    private void addError() {
    	this.errorCounter++;
        if (this.errorCounter > this.maxTimeouts) {
            finish();
        }
    }
    
    /**
     * Shuts down the bot
     */
    public void finish() {
    	super.finish();
    	System.out.println("Bot shut down.");
    }
    
    /**
	 * Updates the time bank for this player, cannot get bigger 
	 * than timebankMax or smaller than zero
	 * @param timeElapsed Time consumed from the time bank
	 */
	private void updateTimeBank(long timeElapsed) {
		this.timebank = Math.max(this.timebank - timeElapsed, 0);
		this.timebank = Math.min(this.timebank + this.timePerMove, this.timebankMax);
	}

    /**
     * Adds the bot's outputs to dump
     * @param dumpy Bot output
     */
    private void botDump(String dumpy) {
        String engineSays = "Output from your bot: \"%s\"";
        addToDump(String.format(engineSays, dumpy));
    }
    
    /**
     * Adds a string to the bot dump
     * @param dumpy String to add to the dump
     */
    public void addToDump(String dumpy) {
        dump.append(dumpy + "\n");
    }
    
    /**
     * @return The dump of all the IO
     */
    public String getDump() {
        return dump.toString();
    }
    
    /**
     * Add a warning to the bot's dump that the engine outputs
     * @param warning The warning message
     */
    public void outputEngineWarning(String warning) {
        dump.append(String.format("Engine warning: \"%s\"\n", warning));
    }
}
