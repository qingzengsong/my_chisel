import chisel3._
import chisel3.util._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

// todo: 扩写这个class,增加可配置项 1.结构和流水方式 2.signed/unsigned
// 关于chisel的scalability的最好例子:single class, all FIRs in the world
class FirFilter(bitWidth: Int, coeffs: Seq[UInt], version: String = "standard") extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(bitWidth.W))
    val out = Output(UInt(bitWidth.W))
  })

  if (version == "standard") { // 标准形式FIR
    val zs = Reg(Vec(coeffs.length, UInt(bitWidth.W)))
    zs(0) := io.in
    for (i <- 1 until coeffs.length) {
      zs(i) := zs(i - 1)
    }

    val products = VecInit.tabulate(coeffs.length)(i => zs(i) * coeffs(i))

    io.out := products.reduce(_ + _)
  }

  if (version == "transpose") {
    val zs = Reg(Vec(coeffs.length, UInt(bitWidth.W)))
    zs(0) := io.in
    for (i <- 1 until coeffs.length) {
      zs(i) := zs(0) * coeffs(coeffs.length - i) + zs(i - 1)
    }
    io.out := zs(coeffs.length - 1)
  }

  if (version == "systolic") {
    val zs = Reg(Vec(coeffs.length, UInt(bitWidth.W)))
    val zs_side = Reg(Vec(coeffs.length, UInt(bitWidth.W)))
    zs(0) := io.in
    zs_side(0) := zs(0) * coeffs(0)
    for (i <- 1 until coeffs.length) {
      zs(i) := zs(i - 1)
      zs_side(i) := zs_side(i - 1) + zs(i) * coeffs(i)
    }
    io.out := zs_side(coeffs.length - 1)
  }
}

class ScalaFirFilter(coeffs: Seq[Int]){
  var pseudoRegisters = List.fill(coeffs.length)(0)

  // 用于模拟输入value一周期后产生的输出
  def poke(value: Int): Int = {
    pseudoRegisters = value::pseudoRegisters.take(coeffs.length - 1) // 移位
    var accumulator = 0
    for(i <- coeffs.indices){
      accumulator += coeffs(i) * pseudoRegisters(i)
    }
    accumulator
  }
}