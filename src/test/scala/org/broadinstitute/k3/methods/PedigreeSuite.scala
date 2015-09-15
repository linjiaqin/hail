package org.broadinstitute.k3.methods

import org.broadinstitute.k3.SparkSuite
import org.testng.annotations.Test

class PedigreeSuite extends SparkSuite {
  @Test def test() {
    val vds = LoadVCF(sc, "sparky", "src/test/resources/sample_mendel.vcf")
    val ped = Pedigree.read("src/test/resources/sample_mendel.fam", vds.sampleIds)
    ped.write("/tmp/sample_mendel.fam", vds.sampleIds)  // FIXME: this is not right
    val pedwr = Pedigree.read("/tmp/sample_mendel.fam", vds.sampleIds)
    assert(ped == pedwr)

    println(ped.nuclearFams)

    val pedComplete = Pedigree(ped.trios.filter(_.isComplete))
    assert(pedComplete.nIndiv == ped.nCompleteTrio)

    assert(ped.nFam == 5 && ped.nIndiv == 11)
    assert(ped.nSatisfying(_.isMale) == 5 && ped.nSatisfying(_.isFemale) == 5)
    assert(ped.nSatisfying(_.isCase) == 4 && ped.nSatisfying(_.isControl) == 3)
    assert(ped.nCompleteTrio == 3 &&
      ped.nSatisfying(_.isComplete, _.isMale) == 2 && ped.nSatisfying(_.isComplete, _.isFemale) == 1 &&
      ped.nSatisfying(_.isComplete, _.isCase) == 2 && ped.nSatisfying(_.isComplete, _.isControl) == 1)
    assert(ped.nSatisfying(_.isComplete, _.isCase, _.isMale) == 1 &&
      ped.nSatisfying(_.isComplete, _.isCase, _.isFemale) == 1 &&
      ped.nSatisfying(_.isComplete, _.isControl, _.isMale) == 1 &&
      ped.nSatisfying(_.isComplete, _.isControl, _.isFemale) == 0)

  }
}
