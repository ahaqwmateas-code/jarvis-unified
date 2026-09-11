import ast, operator

SKILL = {
    "name": "calc",
    "description": "safe math: calc 2+2*10",
    "keywords": ["calc", "calculate", "math", "="],
}

OPS = {
    ast.Add: operator.add, ast.Sub: operator.sub, ast.Mult: operator.mul,
    ast.Div: operator.truediv, ast.FloorDiv: operator.floordiv,
    ast.Mod: operator.mod, ast.Pow: operator.pow,
    ast.USub: operator.neg, ast.UAdd: operator.pos,
}


def _eval(node):
    if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
        return node.value
    if isinstance(node, ast.BinOp) and type(node.op) in OPS:
        return OPS[type(node.op)](_eval(node.left), _eval(node.right))
    if isinstance(node, ast.UnaryOp) and type(node.op) in OPS:
        return OPS[type(node.op)](_eval(node.operand))
    raise ValueError("unsupported expression")


def run(args, ctx):
    expr = args.strip().lstrip("=").strip()
    if not expr:
        return "Usage: calc 2+2*10"
    try:
        tree = ast.parse(expr, mode="eval")
        return expr + " = " + str(_eval(tree.body))
    except Exception as e:
        return "Cannot compute: " + str(e)
