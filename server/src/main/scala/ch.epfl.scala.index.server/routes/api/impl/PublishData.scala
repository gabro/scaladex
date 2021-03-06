package ch.epfl.scala.index
package server
package routes
package api
package impl

import data.{LocalPomRepository, DataPaths}
import data.github

import ch.epfl.scala.index.model.misc.Sha1

import org.joda.time.DateTime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

/**
 * Publish data model / Settings
 * @param path the file name send to scaladex
 * @param created the datime the release was published on the upstream repository
 * @param data the file content
 * @param credentials the credentials (username & password)
 * @param downloadInfo flag for downloading info
 * @param downloadContributors flag for downloading contributors
 * @param downloadReadme flag for downloading the readme file
 */
private[api] case class PublishData(
    path: String,
    created: DateTime,
    data: String,
    credentials: github.Credentials,
    userState: UserState,
    downloadInfo: Boolean,
    downloadContributors: Boolean,
    downloadReadme: Boolean
) {

  lazy val isPom: Boolean = path matches """.*\.pom"""
  lazy val hash = Sha1(data)
  lazy val tempPath = tmpPath(hash)
  def savePath(paths: DataPaths): Path = pomPath(paths, hash)

  /**
   * write the file content to given path
   * @param destination the given destination
   */
  private def write(destination: Path): Unit = {
    delete(destination)
    Files.write(destination, data.getBytes(StandardCharsets.UTF_8))
    ()
  }

  /**
   * delete a given file
   * @param file the file name to delete
   */
  private def delete(file: Path): Unit = {

    if (Files.exists(file)) Files.delete(file)
    ()
  }

  /**
   * write the temp file to /tmp/sha/sha.pom
   */
  def writeTemp(): Unit = write(tempPath)

  /**
   * write the pom file to /index/bintray/pom_sha1
   */
  def writePom(paths: DataPaths): Unit = write(savePath(paths))

  /**
   * delete the temp add file
   */
  def deleteTemp(): Unit = delete(tempPath)

  /**
   * resolve the filename for a specific pom by sha1
   *
   * @param sha1 the sha1 hash of the file
   * @return
   */
  private def pomPath(paths: DataPaths, sha1: String): Path = {
    val repository =
      if (userState.isSonatype) LocalPomRepository.MavenCentral
      else LocalPomRepository.UserProvided

    paths.poms(repository).resolve(s"$sha1.pom")
  }

  /**
   * get the tmp save path for the pom file
   * @param sha1 the sha1 hash
   * @return
   */
  private def tmpPath(sha1: String): Path = {
    val tmpDir = Files.createTempDirectory(sha1)
    Files.createTempFile(tmpDir, "", "")
  }
}
