/*
 * Copyright (C) 2016-2017  Irotsoma, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

/**
 * Created by irotsoma on 4/26/17.
 */
package com.irotsoma.cloudbackenc.filecontroller.files

import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.file.*
import java.util.concurrent.LinkedBlockingQueue
import javax.annotation.PostConstruct

/**
 *
 *
 * @author Justin Zak
 */
@Component
class FileSystemWatcherService {
    /** kotlin-logging implementation*/
    companion object: KLogging()

    @Autowired
    lateinit var watchedLocationRepository: WatchedLocationRepository

    val watchServices = HashMap<FileSystem, WatchService>()

    private val filesToUpdate = LinkedBlockingQueue<WatchEvent<*>>()

    @PostConstruct
    fun initializeService(){
        watchServices.clear()
        watchedLocationRepository.findAll()
            .map { Paths.get(it.path).toAbsolutePath() }
            .forEach {
                if (it.toFile().canRead()){
                    val fileSystem = it.root.fileSystem
                    //create a watch service for each new file system encountered
                    if (!watchServices.containsKey(fileSystem)) {
                        watchServices.put(fileSystem, fileSystem.newWatchService())
                    }
                    it.register(watchServices[fileSystem],StandardWatchEventKinds.ENTRY_CREATE,StandardWatchEventKinds.ENTRY_DELETE,StandardWatchEventKinds.ENTRY_MODIFY)

                } else {
                    logger.debug{"Unable to access registered path: $it"}
                }
            }
        sendFileRequests()
    }

    @Scheduled(fixedDelayString="\$filecontroller.poll.frequency")
    private fun pollWatchers(){
        for (watchService in watchServices){
            try {
                var watchKey = watchService.value.poll()
                do {
                    watchKey.pollEvents().forEach {
                        //remove duplicates and save to be processed
                        if (it.kind().type() is Path) {
                            val path = it.context() as Path
                            //remove already queued instances of this work item
                            filesToUpdate.removeIf{ (it.context() as Path) == path }
                            filesToUpdate.put(it)
                        }
                    }
                    watchKey = watchService.value.poll()
                } while (watchKey != null)
            } catch(e: ClosedWatchServiceException){
                // if the file system goes offline, stop trying to monitor it
                watchServices.remove(watchService.key)
            }
        }
    }

    @Async
    private fun sendFileRequests(){
        //TODO: call central controller with items in queue, needs to be an infinite loop that continues to wait for new items in the queue
    }



}