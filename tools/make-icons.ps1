# 拾简图标生成：同一套几何同时产出矢量（手写 XML）与各密度位图。
# 设计：樱色渐变圆角方底 + 白色“短标题条 + 三行正文条”，呼应应用内 H1 短条元素。
param(
    [string]$OutDir = "E:\project\Android\md\tools\preview",
    [string]$Sizes = "512"
)

Add-Type -AssemblyName System.Drawing

$script:Canvas = 108.0

function New-RoundedPath {
    param([double]$X, [double]$Y, [double]$W, [double]$H, [double]$R)
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    if ($R -le 0.01) {
        $p.AddRectangle((New-Object System.Drawing.RectangleF([float]$X, [float]$Y, [float]$W, [float]$H)))
        return $p
    }
    $d = $R * 2
    $p.AddArc([float]$X, [float]$Y, [float]$d, [float]$d, 180, 90)
    $p.AddArc([float]($X + $W - $d), [float]$Y, [float]$d, [float]$d, 270, 90)
    $p.AddArc([float]($X + $W - $d), [float]($Y + $H - $d), [float]$d, [float]$d, 0, 90)
    $p.AddArc([float]$X, [float]($Y + $H - $d), [float]$d, [float]$d, 90, 90)
    $p.CloseFigure()
    return $p
}

function New-ShijianIcon {
    param([int]$Size, [bool]$Round, [string]$Path, [double]$Zoom = 1.0)

    $bmp = [System.Drawing.Bitmap]::new($Size, $Size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $s = $Size / $script:Canvas

    if ($Round) {
        $shape = New-Object System.Drawing.Drawing2D.GraphicsPath
        $shape.AddEllipse(0, 0, $Size, $Size)
    } else {
        $shape = New-RoundedPath 0 0 $Size $Size ($Size * 0.235)
    }

    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.PointF([float](-0.15 * $Size), [float](-0.15 * $Size))),
        (New-Object System.Drawing.PointF([float](1.15 * $Size), [float](1.15 * $Size))),
        [System.Drawing.Color]::FromArgb(255, 247, 155, 182),
        [System.Drawing.Color]::FromArgb(255, 130, 28, 66)
    )
    $blend = New-Object System.Drawing.Drawing2D.ColorBlend(3)
    $blend.Colors = @(
        [System.Drawing.Color]::FromArgb(255, 247, 155, 182),
        [System.Drawing.Color]::FromArgb(255, 224, 102, 140),
        [System.Drawing.Color]::FromArgb(255, 130, 28, 66)
    )
    $blend.Positions = @(0.0, 0.5, 1.0)
    $grad.InterpolationColors = $blend
    $g.FillPath($grad, $shape)

    $sheen = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.PointF([float](-0.05 * $Size), [float](-0.05 * $Size))),
        (New-Object System.Drawing.PointF([float](1.0 * $Size), [float](1.05 * $Size))),
        [System.Drawing.Color]::FromArgb(46, 255, 255, 255),
        [System.Drawing.Color]::FromArgb(0, 255, 255, 255)
    )
    $g.FillPath($sheen, $shape)

    $white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
    $bars = @(
        @(42.0, 35.0, 24.0, 5.0),
        @(34.0, 48.0, 40.0, 5.0),
        @(34.0, 58.0, 40.0, 5.0),
        @(34.0, 68.0, 26.0, 5.0)
    )
    $center = $script:Canvas / 2.0
    foreach ($b in $bars) {
        $bx = $center + ($b[0] - $center) * $Zoom
        $by = $center + ($b[1] - $center) * $Zoom
        $bw = $b[2] * $Zoom
        $bh = $b[3] * $Zoom
        $p = New-RoundedPath ($bx * $s) ($by * $s) ($bw * $s) ($bh * $s) (2.5 * $Zoom * $s)
        $g.FillPath($white, $p)
        $p.Dispose()
    }

    if (-not (Test-Path $Path)) {
        $dir = Split-Path -Parent $Path
        if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    }
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $white.Dispose(); $grad.Dispose(); $sheen.Dispose(); $shape.Dispose(); $g.Dispose(); $bmp.Dispose()
    Write-Output "wrote $Path"
}

foreach ($size in ($Sizes.Split(',') | Where-Object { $_ -ne '' } | ForEach-Object { [int]$_ })) {
    New-ShijianIcon -Size $size -Round $false -Zoom 1.18 -Path (Join-Path $OutDir "icon-square-$size.png")
    New-ShijianIcon -Size $size -Round $true -Zoom 1.18 -Path (Join-Path $OutDir "icon-round-$size.png")
}
