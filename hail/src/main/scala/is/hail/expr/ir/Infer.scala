package is.hail.expr.ir

import is.hail.expr.types.{virtual, _}
import is.hail.expr.types.virtual._

object Infer {
  def apply(ir: InferIR): Type = {
    ir match {
      case If(cond, cnsq, altr) =>
        assert(cond.typ.isOfType(TBoolean()))
        assert(cnsq.typ.isOfType(altr.typ))
        if (cnsq.typ != altr.typ)
          cnsq.typ.deepOptional()
        else
          cnsq.typ
      case Let(name, value, body) =>
        body.typ
      case ApplyBinaryPrimOp(op, l, r) =>
        BinaryOp.getReturnType(op, l.typ, r.typ)
      case ApplyUnaryPrimOp(op, v) =>
        UnaryOp.getReturnType(op, v.typ)
      case ApplyComparisonOp(op, l, r) =>
        assert(l.typ isOfType r.typ, s"${l.typ.parsableString()} vs ${r.typ.parsableString()}")
        TBoolean()
      case ArrayRef(a, i) =>
        assert(i.typ.isOfType(TInt32()))
        -coerce[TArray](a.typ).elementType
      case ArraySort(a, ascending, _) =>
        assert(ascending.typ.isOfType(TBoolean()))
        a.typ
      case ToSet(a) =>
        TSet(coerce[TArray](a.typ).elementType)
      case ToDict(a) =>
        val elt = coerce[TBaseStruct](coerce[TArray](a.typ).elementType)
        TDict(elt.types(0), elt.types(1))
      case ToArray(a) =>
        TArray(coerce[TContainer](a.typ).elementType)
      case GroupByKey(collection) =>
        val elt = coerce[TBaseStruct](coerce[TArray](collection.typ).elementType)
        TDict(elt.types(0), TArray(elt.types(1)))
      case ArrayMap(a, name, body) =>
        TArray(-body.typ, a.typ.required)
      case ArrayFilter(a, name, cond) =>
        a.typ
      case ArrayFlatMap(a, name, body) =>
        TArray(coerce[TContainer](body.typ).elementType)
      case ArrayFold(a, zero, accumName, valueName, body) =>
        assert(body.typ == zero.typ)
        zero.typ
      case ArrayScan(a, zero, accumName, valueName, body) =>
        assert(body.typ == zero.typ)
        TArray(zero.typ)
      case AggFilter(_, aggIR) =>
        aggIR.typ
      case AggExplode(array, name, aggBody) =>
        aggBody.typ
      case AggGroupBy(key, aggIR) =>
        TDict(key.typ, aggIR.typ)
      case ApplyAggOp(_, _, _, aggSig) =>
        AggOp.getType(aggSig)
      case ApplyScanOp(_, _, _, aggSig) =>
        AggOp.getType(aggSig)
      case MakeStruct(fields) =>
        TStruct(fields.map { case (name, a) =>
          (name, a.typ)
        }: _*)
      case SelectFields(old, fields) =>
        val tbs = coerce[TStruct](old.typ)
        TStruct(fields.map { id => (id, tbs.field(id).typ) }: _*)
      case InsertFields(old, fields) =>
        old.typ.asInstanceOf[TStruct].insertFields(fields.map(x => (x._1, x._2.typ)))
      case GetField(o, name) =>
        val t = coerce[TStruct](o.typ)
        assert(t.index(name).nonEmpty, s"$name not in $t")
        -t.field(name).typ
      case MakeTuple(types) =>
        TTuple(types.map(_.typ): _*)
      case GetTupleElement(o, idx) =>
        val t = coerce[TTuple](o.typ)
        assert(idx >= 0 && idx < t.size)
        -t.types(idx)
      case TableAggregate(child, query) =>
        query.typ
      case MatrixAggregate(child, query) =>
        query.typ
    }
  }
}
