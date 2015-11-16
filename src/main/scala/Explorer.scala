import java.io.File
import actions._
import util._
import core._

/**
 * Created with IntelliJ IDEA.
 * User: yzark
 * Date: 11/19/1
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
object Explorer extends App {
  args.toList match{

    case "populate"::rest             =>
      initializeReaderTables
      PopulateBlockReader
      createIndexes
      closure(PopulateBlockReader.processedBlocks)
      initializeStatsTables
      populateStats

    case "resume"::rest =>
      import sys.process._

      while (new java.io.File("/root/Bitcoin-Graph-Explorer/blockchain/lock").exists)
      {
        "/root/Bitcoin-Graph-Explorer/scripts/getblocklist.sh".! // TODO: replace this script with a scala function entirely!
        val cmd = Seq("cat", "/root/.bitcoin/blocklist.txt") #| Seq( "wc", "-l")
        val from = blockCount
        val to = Integer.parseInt(cmd.lines.head, 10)
        
        if (to > from)
        {
          println("Reading blocks from " + from + " until " + to)
          resume
          // TODO: open LMDB tables in Explorer and keep them open
         
        }
        else
        {
          println("waiting for new blocks")
          Thread sleep 60000
        }
      }
      println("process stopped")


    case _=> println("""
      Available commands:

       populate: create the database movements with movements and closures.
       resume: update the database generated by populate with new incoming data.

    """)
  }

  def closure(blockList:Vector[Int]) = {
    initializeClosureTables
    new PopulateClosure(blockList)
    createAddressIndexes

  }

  def resume = {
    val read = new ResumeBlockReader
    new ResumeClosure(read.processedBlocks)
    println("DEBUG: making new stats")
    resumeStats(read.changedAddresses)
  }

  def resumeStats(changedAddresses: collection.mutable.Map[Hash,Long]) = {
    updateBalanceTables(changedAddresses)
    insertStatistics
    insertRichestAddresses
    insertRichestClosures
  }

  def populateStats = {
    createBalanceTables
    insertStatistics
    insertRichestAddresses
    insertRichestClosures
  }
}
