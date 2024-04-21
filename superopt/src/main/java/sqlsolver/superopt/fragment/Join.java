package sqlsolver.superopt.fragment;

public interface Join extends Op {
  Symbol lhsAttrs();

  Symbol rhsAttrs();

}
