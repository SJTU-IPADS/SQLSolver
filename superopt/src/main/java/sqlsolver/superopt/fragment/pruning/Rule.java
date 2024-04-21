package sqlsolver.superopt.fragment.pruning;

import sqlsolver.superopt.fragment.Fragment;

public interface Rule {
  boolean match(Fragment g);
}
